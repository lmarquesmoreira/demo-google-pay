package br.com.braspag.braspaggooglepay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.*
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class MainActivity : AppCompatActivity() {

    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    private var mPaymentsClient: PaymentsClient? = null

    private var mPwgButton: View? = null

    private val mBikeItem = ItemInfo("Simple Bike", 300 * 1000000)
    private val mShippingCost = (90 * 1000000).toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mPwgButton = button2

        mPwgButton?.setOnClickListener({ view -> requestPayment(view) })

        // It's recommended to create the PaymentsClient object inside of the onCreate method.
        mPaymentsClient = PaymentsUtil.createPaymentsClient(this)
        checkIsReadyToPay()
    }

    private fun checkIsReadyToPay() {
        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        PaymentsUtil.isReadyToPay(mPaymentsClient!!).addOnCompleteListener { task ->
            try {
                val result = task.getResult(ApiException::class.java)
                setPwgAvailable(result)
            } catch (exception: ApiException) {
                // Process error
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    private fun setPwgAvailable(available: Boolean) {
        // If isReadyToPay returned true, show the button and hide the "checking" text. Otherwise,
        // notify the user that Pay with Google is not available.
        // Please adjust to fit in with your current user flow. You are not required to explicitly
        // let the user know if isReadyToPay returns false.
        if (available) {
            mPwgButton?.visibility = View.VISIBLE
        } else {
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val paymentData = PaymentData.getFromIntent(data)
                        handlePaymentSuccess(paymentData)
                    }
                    Activity.RESULT_CANCELED -> {
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        val status = AutoResolveHelper.getStatusFromIntent(data)
                        handleError(status!!.statusCode)
                    }
                }// Nothing to here normally - the user simply cancelled without selecting a
                // payment method.

                // Re-enables the Pay with Google button.
                mPwgButton?.isClickable = true
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData?) {
        // PaymentMethodToken contains the payment information, as well as any additional
        // requested information, such as billing and shipping address.
        //
        // Refer to your processor's documentation on how to proceed from here.
        val token = paymentData!!.paymentMethodToken

        // getPaymentMethodToken will only return null if PaymentMethodTokenizationParameters was
        // not set in the PaymentRequest.
        if (token != null) {
            val billingName = paymentData.cardInfo.billingAddress!!.name
            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show()

            // Use token.getToken() to get the token string.
            Log.d("PaymentData", "PaymentMethodToken received")
        }
    }

    private fun handleError(statusCode: Int) {
        // At this stage, the user has already seen a popup informing them an error occurred.
        // Normally, only logging is required.
        // statusCode will hold the value of any constant from CommonStatusCode or one of the
        // WalletConstants.ERROR_CODE_* constants.
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
    }

    // This method is called when the Pay with Google button is clicked.
    fun requestPayment(view: View) {
        // Disables the button to prevent multiple clicks.
        mPwgButton?.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val price = PaymentsUtil.microsToString(mBikeItem.priceMicros + mShippingCost)

        val transaction = PaymentsUtil.createTransaction(price)
        val request = PaymentsUtil.createPaymentDataRequestDirect(transaction)
        val futurePaymentData = mPaymentsClient!!.loadPaymentData(request)

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        AutoResolveHelper.resolveTask(futurePaymentData, this, LOAD_PAYMENT_DATA_REQUEST_CODE)
    }

//    private fun initItemUI() {
//        val itemName = findViewById(R.id.text_item_name)
//        val itemImage = findViewById(R.id.image_item_image)
//        val itemPrice = findViewById(R.id.text_item_price)
//
//        itemName.setText(mBikeItem.getName())
//        itemImage.setImageResource(mBikeItem.getImageResourceId())
//        itemPrice.setText(PaymentsUtil.microsToString(mBikeItem.getPriceMicros()))
//    }
}

class ItemInfo(val name: String,
               val priceMicros: Long)

object PaymentsUtil {
    private val MICROS = BigDecimal(1000000.0)

    /**
     * Creates an instance of [PaymentsClient] for use in an [Activity] using the
     * environment and theme set in [Constants].
     *
     * @param activity is the caller's activity.
     */
    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
                .build()
        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    /**
     * Builds [PaymentDataRequest] to be consumed by [PaymentsClient.loadPaymentData].
     *
     * @param transactionInfo contains the price for this transaction.
     */
//    fun createPaymentDataRequest(transactionInfo: TransactionInfo): PaymentDataRequest {
//        val paramsBuilder = PaymentMethodTokenizationParameters.newBuilder()
//                .setPaymentMethodTokenizationType(
//                        WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
//                .addParameter("gateway", Constants.GATEWAY_TOKENIZATION_NAME)
//        for (param in Constants.GATEWAY_TOKENIZATION_PARAMETERS) {
//            paramsBuilder.addParameter(param.first, param.second)
//        }
//
//        return createPaymentDataRequest(transactionInfo, paramsBuilder.build())
//    }

    /**
     * Builds [PaymentDataRequest] for use with DIRECT integration to be consumed by
     * [PaymentsClient.loadPaymentData].
     *
     *
     * Please refer to the documentation for more information about DIRECT integration. The type of
     * integration you use depends on your payment processor.
     *
     * @param transactionInfo contains the price for this transaction.
     */
    fun createPaymentDataRequestDirect(transactionInfo: TransactionInfo): PaymentDataRequest {
        val params = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(
                        WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_DIRECT)

                // Omitting the publicKey will result in a request for unencrypted data.
                // Please refer to the documentation for more information on unencrypted
                // requests.
                .addParameter("publicKey", Constants.DIRECT_TOKENIZATION_PUBLIC_KEY)
                .build()

        return createPaymentDataRequest(transactionInfo, params)
    }

    private fun createPaymentDataRequest(transactionInfo: TransactionInfo, params: PaymentMethodTokenizationParameters): PaymentDataRequest {

        return PaymentDataRequest.newBuilder()
                .setPhoneNumberRequired(false)
                .setEmailRequired(true)
                .setShippingAddressRequired(false)

                // Omitting ShippingAddressRequirements all together means all countries are
                // supported.
                .setShippingAddressRequirements(
                        ShippingAddressRequirements.newBuilder()
                                .addAllowedCountryCodes(Constants.SHIPPING_SUPPORTED_COUNTRIES)
                                .build())

                .setTransactionInfo(transactionInfo)
                .addAllowedPaymentMethods(Constants.SUPPORTED_METHODS)
                .setCardRequirements(
                        CardRequirements.newBuilder()
                                .addAllowedCardNetworks(Constants.SUPPORTED_NETWORKS)
                                .setAllowPrepaidCards(true)
                                .setBillingAddressRequired(true)

                                // Omitting this parameter will result in the API returning
                                // only a "minimal" billing address (post code only).
                                .setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                                .build())
                .setPaymentMethodTokenizationParameters(params)

                // If the UI is not required, a returning user will not be asked to select
                // a card. Instead, the card they previously used will be returned
                // automatically (if still available).
                // Prior whitelisting is required to use this feature.
                .setUiRequired(true)
                .build()
    }

    /**
     * Determines if the user is eligible to Pay with Google by calling
     * [PaymentsClient.isReadyToPay]. The nature of this check depends on the methods set in
     * [Constants.SUPPORTED_METHODS].
     *
     *
     * If [WalletConstants.PAYMENT_METHOD_CARD] is specified among supported methods, this
     * function will return true even if the user has no cards stored. Please refer to the
     * documentation for more information on how the check is performed.
     *
     * @param client used to send the request.
     */
    fun isReadyToPay(client: PaymentsClient): Task<Boolean> {
        val request = IsReadyToPayRequest.newBuilder()
        for (allowedMethod in Constants.SUPPORTED_METHODS) {
            request.addAllowedPaymentMethod(allowedMethod!!)
        }
        return client.isReadyToPay(request.build())
    }

    /**
     * Builds [TransactionInfo] for use with [PaymentsUtil.createPaymentDataRequest].
     *
     *
     * The price is not displayed to the user and must be in the following format: "12.34".
     * [PaymentsUtil.microsToString] can be used to format the string.
     *
     * @param price total of the transaction.
     */
    fun createTransaction(price: String): TransactionInfo {
        return TransactionInfo.newBuilder()
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setTotalPrice(price)
                .setCurrencyCode(Constants.CURRENCY_CODE)
                .build()
    }

    /**
     * Converts micros to a string format accepted by [PaymentsUtil.createTransaction].
     *
     * @param micros value of the price.
     */
    fun microsToString(micros: Long): String {
        return BigDecimal(micros).divide(MICROS).setScale(2, RoundingMode.HALF_EVEN).toString()
    }
}

object Constants {
    // This file contains several constants you must edit before proceeding. Once you're done,
    // remove this static block and run the sample.
    // Before you start, please take a look at PaymentsUtil.java to see where the constants are used
    // and to potentially remove ones not relevant to your integration.
    // Required changes:
    // 1.  Update SUPPORTED_NETWORKS and SUPPORTED_METHODS if required (consult your processor if
    //     unsure).
    // 2.  Update CURRENCY_CODE to the currency you use.
    // 3.  Update SHIPPING_SUPPORTED_COUNTRIES to list the countries where you currently ship. If
    //     this is not applicable to your app, remove the relevant bits from PaymentsUtil.java.
    // 4.  If you're integrating with your processor / gateway directly, update
    //     GATEWAY_TOKENIZATION_NAME and GATEWAY_TOKENIZATION_PARAMETERS per the instructions they
    //     provided. You don't need to update DIRECT_TOKENIZATION_PUBLIC_KEY.
    // 5.  If you're using direct integration, please consult the documentation to learn about
    //     next steps.
    //    static {
    //        if (true) {
    //            throw new RuntimeException("[REMOVE ME] Please edit the Constants.java file per the"
    //                    + " instructions inside before trying to run this sample.");
    //        }
    //    }

    // Changing this to ENVIRONMENT_PRODUCTION will make the API return real card information.
    // Please refer to the documentation to read about the required steps needed to enable
    // ENVIRONMENT_PRODUCTION.
    val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

    // The allowed networks to be requested from the API. If the user has cards from networks not
    // specified here in their account, these will not be offered for them to choose in the popup.
    val SUPPORTED_NETWORKS = Arrays.asList(
            WalletConstants.CARD_NETWORK_AMEX,
            WalletConstants.CARD_NETWORK_DISCOVER,
            WalletConstants.CARD_NETWORK_VISA,
            WalletConstants.CARD_NETWORK_MASTERCARD
    )

    val SUPPORTED_METHODS = Arrays.asList(
            // PAYMENT_METHOD_CARD returns to any card the user has stored in their Google Account.
            WalletConstants.PAYMENT_METHOD_CARD,

            // PAYMENT_METHOD_TOKENIZED_CARD refers to cards added to Android Pay, assuming Android
            // Pay is installed.
            // Please keep in mind cards may exist in Android Pay without being added to the Google
            // Account.
            WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD
    )

    // Required by the API, but not visible to the user.
    val CURRENCY_CODE = "BRL"

    // Supported countries for shipping (use ISO 3166-1 alpha-2 country codes).
    // Relevant only when requesting a shipping address.
    val SHIPPING_SUPPORTED_COUNTRIES = listOf("BR")

    // The name of your payment processor / gateway. Please refer to their documentation for
    // more information.
    //val GATEWAY_TOKENIZATION_NAME = "REPLACE_ME"

    // Custom parameters required by the processor / gateway.
    // In many cases, your processor / gateway will only require a gatewayMerchantId.
    // Please refer to your processor's documentation for more information. The number of parameters
    // required and their names vary depending on the processor.
//    val GATEWAY_TOKENIZATION_PARAMETERS = Arrays.asList(
//            Pair.create("gatewayMerchantId", "REPLACE_ME")
//    )

    // Example configuration when using Stripe as processor.
    //public static final String GATEWAY_TOKENIZATION_NAME = "stripe";
    //public static final List<Pair<String, String>> GATEWAY_TOKENIZATION_PARAMETERS = Arrays.asList(
    //        Pair.create("stripe:version", "5.0.0"),
    //        Pair.create("stripe:publishableKey", "pk_test_yourstripetestkey")
    //);

    // Only used for DIRECT tokenization. Can be removed when using GATEWAY tokenization.
    val DIRECT_TOKENIZATION_PUBLIC_KEY = "BBKRQbmMG6EcSDeFEM3Fk80Sr99DYicfSoMzBtarQgkXjfFyY5wu6ytkQRDM2L1LTH/9zY69EThW5NfVm2NLlbY="


}
