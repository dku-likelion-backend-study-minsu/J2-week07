package com.ll.demo03.domain.surl.surl.repository;

import com.ll.demo03.domain.surl.surl.entity.Surl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurlRepository extends JpaRepository<Surl, Long> {

}