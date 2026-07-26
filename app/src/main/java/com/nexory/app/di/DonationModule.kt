package com.nexory.app.di

import com.nexory.app.data.donation.BankLinkDonationService
import com.nexory.app.data.donation.DonationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Привязка реализации пожертвований.
 *
 * Единственное место, которое нужно поменять при переходе на платёжного агрегатора:
 * заменить [BankLinkDonationService] на, например, YooKassaDonationService.
 * Экраны и ViewModel работают через интерфейс [DonationService] и правок не потребуют.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DonationModule {

    @Binds
    @Singleton
    abstract fun bindDonationService(impl: BankLinkDonationService): DonationService
}
