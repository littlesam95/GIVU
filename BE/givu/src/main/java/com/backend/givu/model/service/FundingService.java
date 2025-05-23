package com.backend.givu.model.service;

import com.backend.givu.model.Document.FundingDocument;
import com.backend.givu.model.Enum.FundingsScope;
import com.backend.givu.model.Enum.FundingsStatus;
import com.backend.givu.model.Enum.PaymentsStatus;
import com.backend.givu.model.Enum.PaymentsTransactionType;
import com.backend.givu.model.entity.*;
import com.backend.givu.model.repository.*;
import com.backend.givu.model.requestDTO.FundingCreateDTO;
import com.backend.givu.model.requestDTO.FundingUpdateDTO;
import com.backend.givu.model.responseDTO.*;
import com.backend.givu.util.mapper.CategoryMapper;
import com.backend.givu.util.mapper.FundingMapper;
import com.backend.givu.util.mapper.ScopeMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class FundingService {
    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;
    private final FundingSearchRepository fundingSearchRepository;
    private final ProductRepository productRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    private final S3UploadService s3UploadService;

    /**
     * 펀딩 Id로 조회
     */
    public Funding findFundingEntity(int fundingId){
        return fundingRepository.findById(fundingId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));
    }

    /**
     * 펀딩 이미지 저장
     */
    public void saveFundingEntity(Funding fundig){
        fundingRepository.save(fundig);
    }


    public List<FundingsDTO> findAllFunding(Long userId) {
        List<Funding> fundings = fundingRepository.findAllWithUserAndProduct();

        List<Long> friendIds = userId != null
                ? friendRepository.findByUserWithFriend(userId).stream()
                .map(f -> f.getFriend().getId())
                .toList()
                : List.of();

        return fundings.stream()
                .map(funding -> {
                    Long ownerId = funding.getUser().getId();

                    boolean hidden = false;
                    if (funding.getScope() == FundingsScope.PRIVATE) {
                        hidden = userId == null || !(ownerId.equals(userId) || friendIds.contains(ownerId));
                    }

                    return new FundingsDTO(funding, hidden);
                })
                .toList();
    }




    /**
     * 검색한 펀딩 리스트 조회
     */
    public ApiResponse<List<FundingsDTO>> findAllSearchFunding(String title, Long userId) {
        List<FundingDocument> fundingDocuments = fundingSearchRepository.searchFundingByKeyword(title);

        Set<Long> friendIds = userId != null
                ? friendRepository.findByUserWithFriend(userId).stream()
                .map(f -> f.getFriend().getId())
                .collect(Collectors.toSet())
                : Set.of();

        List<FundingsDTO> result = fundingDocuments.stream()
                .map(doc -> {
                    Long ownerId = doc.getUserId();

                    boolean hidden = false;
                    if (FundingsScope.valueOf(doc.getScope()) == FundingsScope.PRIVATE) {
                        hidden = userId == null || !(ownerId.equals(userId) || friendIds.contains(ownerId));
                    }

                    return new FundingsDTO(doc, hidden);
                })
                .toList();

        return ApiResponse.success(result);
    }





    /**
     * 내가 만든 펀딩 리스트 조회
     */
    public ApiResponse<List<FundingsDTO>> findAllMyFunding(long userId){
        List<Funding> fundings =  fundingRepository.findAllMyFundingWithUserAndProduct(userId);
        return ApiResponse.success(fundings.stream()
                .map(FundingsDTO::new)
                .toList());
    }

    /**
     * 내가 참여한 펀딩 리스트 조회
     */
    public ApiResponse<List<FundingsDTO>> findAllMyParticipantFunding(long userId){
        List<Funding> fundings =  fundingRepository.findMyParticipantFunding(userId);
        return ApiResponse.success(fundings.stream()
                .map(FundingsDTO::new)
                .toList());
    }

    /**
     * 펀딩 생성
     */
    @Transactional
    public FundingsDTO saveFunding (Long userId, FundingCreateDTO fundingDTO, List<String > imageUrls){
        // 존재하는 유저인지 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
        // 존재하는 상품인지 확인
        Product product = productRepository.findById(fundingDTO.getProductId())
                .orElseThrow(()-> new EntityNotFoundException("상품을 찾을 수 없습니다."));
        Funding saveFunding = fundingRepository.save(Funding.from(user, product, fundingDTO, imageUrls));
        indexFundingsToElasticsearch(saveFunding);
        return  Funding.toDTO(saveFunding);

    }

    /**
     * 펀딩 수정
     */
    @Transactional
    public FundingsDTO updateFunding (Long userId, Integer fundingId, FundingUpdateDTO fundingDTO, List<String> imageUrls)
            throws AccessDeniedException {

        // 존재하는 펀딩인지 확인
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new EntityNotFoundException("펀딩을 찾을 수 없습니다,"));

        // 본인 펀딩인지 확인
        log.info("수정 요청보낸 유저ID: " + userId);
        if(!funding.getUser().getId().equals(userId)){
            log.info("펀딩 주인 ID: "+ funding.getUser().getId());
            throw new AccessDeniedException("펀딩 수정 권한이 없습니다.");
        }
        // DTO 내용 entity에 넣기
        funding.setTitle(fundingDTO.getTitle());
        funding.setDescription(fundingDTO.getDescription());
        funding.setCategory(CategoryMapper.fromClient(fundingDTO.getCategory())); // 한글 -> 영어
        funding.setScope(ScopeMapper.fromClient(fundingDTO.getScope()));          // 한글 -> 영어


        // 기존 이미지 중 삭제요청된 거만 삭제
        if(fundingDTO.getToDelete() != null){
            // S3에 사진 삭제
            s3UploadService.deleteFile(fundingDTO.getToDelete());
            // DB에서 사진 URL 삭제
            List<String> imageList = new ArrayList<>(funding.getImage());
            imageList.removeAll(fundingDTO.getToDelete());
            funding.setImage(imageList);
        }

        // 추가된 이미지 업로드
        if (imageUrls != null){
            for(String url: imageUrls){
                if(url != null && !url.isEmpty()){
                    funding.addImage(url);
                }
            }
        }
        indexFundingsToElasticsearch(funding);
        return Funding.toDTO(funding);
    }

    /**
     * 펀딩 삭제
     */
    public void deleteFunding (Long userId, int fundingId) throws AccessDeniedException{
        // 존재하는 펀딩인지 확인
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new EntityNotFoundException("펀딩을 찾을 수 없습니다,"));

        // 본인 펀딩인지 확인
        if(!funding.getUser().getId().equals(userId)){
            log.warn("펀딩 삭제 권한 없음: 요청자={}, 작성자={}", userId, funding.getUser().getId());
            throw new AccessDeniedException("펀딩 삭제 권한이 없습니다.");
        }

        // S3에서 이미지 URL 삭제
        if(funding.getImage() != null && !funding.getImage().isEmpty()){
            s3UploadService.deleteFile(funding.getImage());
        }

        fundingRepository.deleteById(fundingId);
        fundingSearchRepository.deleteById(fundingId);
    }



    /**
     * 펀딩 상세 보기
     */
    public ApiResponse<FundingDetailDTO> fundingDetail(Long userId, int fundingId) {

        // 펀딩 + 유저 + 상품 가져오기
        Funding funding = fundingRepository.findByIdWithUserAndProduct(fundingId)
                .orElseThrow(() -> new EntityNotFoundException("펀딩을 찾을 수 없습니다."));

        Product product = funding.getProduct();
        User writer = funding.getUser();

        boolean isCreator = false;
        Long viewerId = null;

        if (userId != null) {
            // 로그인 유저일 경우에만 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
            isCreator = user.getId().equals(writer.getId());
            viewerId = user.getId();
        }

        // 편지 + 후기
        List<Letter> letters = fundingRepository.findLetterByFundingIdWithUser(fundingId);
        List<Review> reviews = fundingRepository.findReviewByFundingIdWithUser(fundingId);

        // 참가자 리스트
        List<Participant> participantEntities = participantRepository.findByFundingIdWithUser(fundingId);
        List<User> participants = participantEntities.stream()
                .map(Participant::getUser)
//                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // DTO 조립
        FundingDetailDTO dto = FundingDetailDTO.of(
                funding, isCreator, writer, product, letters, reviews, viewerId,participants);

//        FundingDetailDTO dto = FundingDetailDTO.of(funding, isCreator, writer, product, letters, reviews, viewerId);


        return ApiResponse.success(dto);
    }


    /**
     * 펀딩 결제 확인
     */
    public PaymentResultDTO paymentResult(Long userId, int transactionId){

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new EntityNotFoundException("유저를 찾을 수 없습니다."));
        Payment payment = paymentRepository.findById(transactionId)
                .orElseThrow(()-> new EntityNotFoundException("해당 결제 이력을 찾을 수 없습니다."));

        // 유저가 본인 결제인지 검증 (선택사항)
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 결제는 요청한 유저의 결제가 아닙니다.");
        }

        return new PaymentResultDTO(payment);


    }


    @Transactional(readOnly = true)
    public void indexAllFundingsToElasticsearch() {
        List<Funding> fundings = fundingRepository.findAllWithUserAndProduct();

        List<FundingDocument> documents = fundings.stream()
                .map(FundingMapper::toDocument)
                .toList();

        fundingSearchRepository.saveAll(documents); // ES에 대량 색인
    }

    @Transactional(readOnly = true)
    public void indexFundingsToElasticsearch(Funding funding){
        FundingDocument fundingDocument = FundingMapper.toDocument(funding);
        fundingSearchRepository.save(fundingDocument);
    }


    /**
     * 펀딩 완료
     */
    public FundingsDTO fundingPurchase(Long userId, int fundingId, String address){
        // 존재하는 펀딩인지 확인
        Funding funding = fundingRepository.findByIdWithUserAndProduct(fundingId)
                .orElseThrow(() -> new EntityNotFoundException("펀딩을 찾을 수 없습니다,"));

        // 본인 펀딩인지 확인
        if(!funding.getUser().getId().equals(userId)) {
            log.warn("펀딩 완료 권한 없음: 요청자={}, 작성자={}", userId, funding.getUser().getId());
            throw new AccessDeniedException("펀딩 완료 권한이 없습니다.");
        }

        // 펀딩 모금액과 상품 가격이 같은 지 또는 complete 상태 인지
        if(!funding.getFundedAmount().equals(funding.getProduct().getPrice())
                || funding.getStatus() != FundingsStatus.COMPLETED){
            log.warn("펀딩 완료 상태가 아님: 모금액={}, 상품가격={}, 상태={}",
                    funding.getFundedAmount(), funding.getProduct().getPrice(),funding.getStatus());
            throw new IllegalArgumentException("펀딩 완료 상태가 아닙니다.");
        }

        // 주소가  null 이 아닌지 확인
        if(address==null){
            log.warn("올바른 주소 아님: 주소={}", address);
            throw new IllegalArgumentException("올바른 주소를 입력하세요");
        }

        // 배송 중 상태로 변경
        funding.setStatus(FundingsStatus.SHIPPING);

        // payment 생성
        Payment payment = Payment.builder()
                .user(funding.getUser())
                .relatedFunding(funding)
                .relatedProduct(funding.getProduct())
                .amount(funding.getFundedAmount())
                .transactionType(PaymentsTransactionType.PRODUCT)
                .status(PaymentsStatus.SUCCESS)
                .build();

        paymentRepository.save(payment);

        // 변경 사항 저장 및 DTO로 변환
        return Funding.toDTO(fundingRepository.save(funding));

    }







}
