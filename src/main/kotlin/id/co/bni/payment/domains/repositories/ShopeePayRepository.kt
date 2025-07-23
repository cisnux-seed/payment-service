package id.co.bni.payment.domains.repositories

import id.co.bni.payment.domains.dtos.GetShopeePayWalletReq
import id.co.bni.payment.domains.dtos.GetShopeePayWalletResp
import id.co.bni.payment.domains.dtos.ShopeePayTopUpReq
import id.co.bni.payment.domains.dtos.ShopeePayTopUpResp

interface ShopeePayRepository {
    suspend fun getShopeePayBalance(req: GetShopeePayWalletReq): GetShopeePayWalletResp
    suspend fun topUp(req: ShopeePayTopUpReq): ShopeePayTopUpResp
}