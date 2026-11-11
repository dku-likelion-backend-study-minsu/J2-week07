package com.ll.demo03.domain.member.member.controller;

import com.ll.demo03.domain.member.member.entity.Member;
import com.ll.demo03.domain.member.member.service.MemberService;
import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.util.Ut;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class ApiV1MemberController {
    private final MemberService memberService;

    @AllArgsConstructor
    @Getter
    public static class MemberJoinReqBody {
        private String username;
        private String password;
        private String nickname;
    }

    @PostMapping("")
    public RsData<Member> join(
            @RequestBody MemberJoinReqBody reqBody
    ) {
        if (Ut.str.isBlank(reqBody.username)) {
            throw new GlobalException("400-1", "아이디를 입력해주세요.");
        }

        if (Ut.str.isBlank(reqBody.password)) {
            throw new GlobalException("400-2", "비밀번호를 입력해주세요.");
        }

        if (Ut.str.isBlank(reqBody.nickname)) {
            throw new GlobalException("400-3", "닉네임을 입력해주세요.");
        }

        return memberService.join(reqBody.username, reqBody.password, reqBody.nickname);
    }
}
