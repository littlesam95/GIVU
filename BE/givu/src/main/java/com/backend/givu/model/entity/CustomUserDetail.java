package com.backend.givu.model.entity;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 유저의 정보를 가져오는 UserDetails 인터페이스를 상속하는 클래스
 * - Authentication을 담고 있음.
 */
public class CustomUserDetail implements UserDetails {

    private final User user;

    // 생성자
    public CustomUserDetail(User user){
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER")); // 기본 권한 부여
    }

    public Long getId() {
        return user.getId();
    }
    public User getUser(){return user;}

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public String getAccountNo(){
        return user.getAccountNumber() == null ? "" : user.getAccountNumber();
    }

    /* 세부 설정 */

    @Override
    public boolean isAccountNonExpired() {  // 계정의 만료 여부
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {   // 계정의 잠김 여부
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {  // 비밀번호 만료 여부
        return true;
    }

    @Override
    public boolean isEnabled() {    // 계정의 활성화 여부
        return true;
    }





}
