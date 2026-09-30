package com.teachquest.service;

public record CertificateIssuedEvent(Long userId, Long courseId, String tier, String previousTier) { }