package id.co.bni.payment.domains.repositories

import id.co.bni.payment.domains.dtos.CommonDtoResp
import id.co.bni.payment.domains.dtos.GopayTopUpReq
import id.co.bni.payment.domains.dtos.GopayTopUpResp
import id.co.bni.payment.domains.dtos.GopayWalletResp

interface GopayRepository {
    suspend fun getBalanceByWalletId(walletId: String): CommonDtoResp<GopayWalletResp>
    suspend fun topUp(topUpReq: GopayTopUpReq): GopayTopUpResp?
}