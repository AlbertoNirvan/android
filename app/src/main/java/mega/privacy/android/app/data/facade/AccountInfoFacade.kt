package mega.privacy.android.app.data.facade

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.iab.BillingConstant.PAYMENT_GATEWAY
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.domain.entity.billing.MegaPurchase
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account info facade
 *
 * Implements [AccountInfoWrapper] and provides a facade over [MyAccountInfo]
 *
 * @property myAccountInfo
 */
@Singleton
class AccountInfoFacade @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val db: DatabaseHandler,
) : AccountInfoWrapper {
    private val accountDetail = MutableSharedFlow<MegaAccountDetails>(replay = 1)

    override val storageCapacityUsedAsFormattedString: String
        get() = myAccountInfo.usedFormatted
    override val accountTypeId: Int
        get() = myAccountInfo.accountType
    override val accountTypeString: String
        get() = getAccountTypeLabel(myAccountInfo.accountType)

    override suspend fun handleAccountDetail(request: MegaRequest) {
        Timber.d("Account details request")
        val storage = request.numDetails and MyAccountInfo.HAS_STORAGE_DETAILS != 0
        if (storage && megaApiGateway.getRootNode() != null) {
            db.setAccountDetailsTimeStamp()
        }
        val megaAccountDetails = request.megaAccountDetails ?: return
        // backward compatible, it will replace by new AccountDetail domain entity
        myAccountInfo.setAccountInfo(megaAccountDetails)
        myAccountInfo.setAccountDetails(request.numDetails)
        val sessions =
            request.numDetails and MyAccountInfo.HAS_SESSIONS_DETAILS != 0
        if (sessions) {
            val megaAccountSession = megaAccountDetails.getSession(0) ?: return
            Timber.d("getMegaAccountSESSION not Null")
            db.setExtendedAccountDetailsTimestamp()
            val mostRecentSession: Long = megaAccountSession.mostRecentUsage
            val date: String = TimeUtils.formatDateAndTime(context,
                mostRecentSession,
                TimeUtils.DATE_LONG_FORMAT)
            myAccountInfo.lastSessionFormattedDate = date
            myAccountInfo.createSessionTimeStamp = megaAccountSession.creationTimestamp
            Timber.d("onRequest TYPE_ACCOUNT_DETAILS: %s", myAccountInfo.usedPercentage)
        }
        sendBroadcastUpdateAccountDetails()
        accountDetail.emit(request.megaAccountDetails)
    }

    override val activeSubscription: MegaPurchase?
        get() = myAccountInfo.activeSubscription

    override val subscriptionMethodId: Int
        get() = myAccountInfo.subscriptionMethodId

    override fun updateActiveSubscription(purchase: MegaPurchase?, levelInventory: Int) {
        Timber.d("Set current max subscription: $purchase")
        myAccountInfo.activeSubscription = purchase

        myAccountInfo.levelInventory = levelInventory
        myAccountInfo.isInventoryFinished = true
        purchase?.let {
            updateSubscriptionLevel(it)
        }
    }

    private fun updateSubscriptionLevel(purchase: MegaPurchase) {
        val json = purchase.receipt
        Timber.d("ORIGINAL JSON:$json") //Print JSON in logs to help debug possible payments issues

        val attributes: MegaAttributes? = db.attributes

        val lastPublicHandle = attributes?.lastPublicHandle ?: megaApiGateway.getInvalidHandle()
        val listener = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _, error ->
                if (error.errorCode != MegaError.API_OK) {
                    Timber.e("PURCHASE WRONG: ${error.errorString} (${error.errorCode})")
                }
            }
        )

        if (myAccountInfo.levelInventory > myAccountInfo.levelAccountDetails) {
            Timber.d("megaApi.submitPurchaseReceipt is invoked")
            if (lastPublicHandle == MegaApiJava.INVALID_HANDLE) {
                megaApiGateway.submitPurchaseReceipt(PAYMENT_GATEWAY, json, listener)
            } else {
                attributes?.run {
                    megaApiGateway.submitPurchaseReceipt(PAYMENT_GATEWAY, json, lastPublicHandle,
                        lastPublicHandleType, lastPublicHandleTimeStamp, listener
                    )
                }
            }
        }
    }

    // we have to continue send broadcast receiver it will replace by accountDetail SharedFlow
    private fun sendBroadcastUpdateAccountDetails() {
        context.sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            .putExtra(BroadcastConstants.ACTION_TYPE, Constants.UPDATE_ACCOUNT_DETAILS))
    }

    private fun getAccountTypeLabel(accountType: Int?) = with(context) {
        when (accountType) {
            Constants.FREE -> getString(R.string.my_account_free)
            Constants.PRO_I -> getString(R.string.my_account_pro1)
            Constants.PRO_II -> getString(R.string.my_account_pro2)
            Constants.PRO_III -> getString(R.string.my_account_pro3)
            Constants.PRO_LITE -> getString(R.string.my_account_prolite_feedback_email)
            Constants.BUSINESS -> getString(R.string.business_label)
            else -> getString(R.string.my_account_free)
        }
    }
}