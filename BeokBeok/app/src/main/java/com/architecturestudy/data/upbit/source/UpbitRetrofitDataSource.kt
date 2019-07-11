package com.architecturestudy.data.upbit.source

import com.architecturestudy.data.common.MarketTypes
import com.architecturestudy.data.upbit.UpbitTicker
import com.architecturestudy.data.upbit.service.UpbitService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class UpbitRetrofitDataSource private constructor(
    private val retrofit: UpbitService
) : UpbitDataSource {

    override fun getMarketPrice(
        prefix: String,
        onSuccess: (List<UpbitTicker>) -> Unit,
        onFail: (Throwable) -> Unit
    ) {
        CompositeDisposable().add(
            retrofit.getMarkets()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val markets = it.body()
                    if (markets.isNullOrEmpty()) {
                        onFail(IllegalStateException("Data not validate"))
                    } else {
                        val tickers = markets
                            .asSequence()
                            .filter {
                                enumValues<MarketTypes>().any { data ->
                                    data.name == prefix
                                }
                            }
                            .filter { data -> data.market!!.startsWith(prefix) }
                            .map { data -> data.market }
                            .toList()
                        getTickers(
                            tickers,
                            onSuccess,
                            onFail
                        )
                    }
                }, {
                    onFail(it)
                })
        )
    }

    private fun getTickers(
        tickers: List<String?>?,
        onSuccess: (List<UpbitTicker>) -> Unit,
        onFail: (Throwable) -> Unit
    ) {
        CompositeDisposable().add(
            retrofit.getTicker(tickers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val responseTicker = it.body()
                    if (responseTicker.isNullOrEmpty()) {
                        onFail(IllegalStateException("Data is empty"))
                    } else {
                        onSuccess(responseTicker)
                    }
                }, {
                    onFail(it)
                })
        )
    }

    companion object {
        private var instance: UpbitRetrofitDataSource? = null

        operator fun invoke(
            retrofit: UpbitService
        ): UpbitRetrofitDataSource =
            instance ?: UpbitRetrofitDataSource(retrofit)
                .apply { instance = this }

    }
}