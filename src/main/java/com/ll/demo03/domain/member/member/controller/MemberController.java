package com.ll.demo03.domain.member.member.controller;

import com.ll.demo03.domain.member.member.entity.Member;
import com.ll.demo03.domain.member.member.service.MemberService;
import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.util.Ut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/join")
    @ResponseBody
    public RsData<Member> join(
            String username, String password, String nickname
    ) {
        if (Ut.str.isBlank(username)) {
            throw new GlobalException("400-1", "아이디를 입력해주세요.");
        }

        if (Ut.str.isBlank(password)) {
            throw new GlobalException("400-2", "비밀번호를 입력해주세요.");
        }

        if (Ut.str.isBlank(nickname)) {
            throw new GlobalException("400-3", "닉네임을 입력해주세요.");
        }

        return memberService.join(username, password, nickname);
    }

    @GetMapping("/testThrowIllegalArgumentException")
    @ResponseBody
    public RsData<Member> testThrowIllegalArgumentException() {
        throw new IllegalArgumentException("IllegalArgumentException");
    }
}
