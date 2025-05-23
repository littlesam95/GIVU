package com.backend.givu.model.responseDTO;

import com.backend.givu.model.Enum.PaymentsStatus;
import com.backend.givu.model.Enum.PaymentsTransactionType;
import com.backend.givu.model.entity.Payment;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentsDTO {
    private int transactionId;
    private long userId;
    private int relatedFundingId;
    private int relatedProductId;
    private PaymentsTransactionType transactionType;
    private int amount;
    private PaymentsStatus status;
    private LocalDateTime date;


}
