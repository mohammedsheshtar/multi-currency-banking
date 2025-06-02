package com.bank.TransferLink

// TransferLinkRepository.kt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TransferLinkRepository : JpaRepository<TransferLinkEntity, Long> {
    fun findByLinkId(linkId: String): TransferLinkEntity?
}