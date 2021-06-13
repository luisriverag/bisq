package bisq.core.api;

import bisq.core.offer.OpenOffer;

import bisq.proto.grpc.EditOfferRequest;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

@Slf4j
class EditOfferValidator {

    private final OpenOffer currentlyOpenOffer;
    private final String editedPriceAsString;
    private final boolean editedUseMarketBasedPrice;
    private final double editedMarketPriceMargin;
    private final long editedTriggerPrice;
    private final EditOfferRequest.EditType editType;

    private final boolean isZeroEditedFixedPriceString;
    private final boolean isZeroEditedMarketPriceMargin;
    private final boolean isZeroEditedTriggerPrice;

    EditOfferValidator(OpenOffer currentlyOpenOffer,
                       String editedPriceAsString,
                       boolean editedUseMarketBasedPrice,
                       double editedMarketPriceMargin,
                       long editedTriggerPrice,
                       EditOfferRequest.EditType editType) {
        this.currentlyOpenOffer = currentlyOpenOffer;
        this.editedPriceAsString = editedPriceAsString;
        this.editedUseMarketBasedPrice = editedUseMarketBasedPrice;
        this.editedMarketPriceMargin = editedMarketPriceMargin;
        this.editedTriggerPrice = editedTriggerPrice;
        this.editType = editType;

        this.isZeroEditedFixedPriceString = new BigDecimal(editedPriceAsString).doubleValue() == 0;
        this.isZeroEditedMarketPriceMargin = editedMarketPriceMargin == 0;
        this.isZeroEditedTriggerPrice = editedTriggerPrice == 0;
    }

    void validate() {
        log.info("Verifying 'editoffer' params OK for editType {}", editType);
        switch (editType) {
            case ACTIVATION_STATE_ONLY: {
                validateEditedActivationState();
                break;
            }
            case FIXED_PRICE_ONLY:
            case FIXED_PRICE_AND_ACTIVATION_STATE: {
                validateEditedFixedPrice();
                break;
            }
            case MKT_PRICE_MARGIN_ONLY:
            case MKT_PRICE_MARGIN_AND_ACTIVATION_STATE:
            case TRIGGER_PRICE_ONLY:
            case TRIGGER_PRICE_AND_ACTIVATION_STATE:
            case MKT_PRICE_MARGIN_AND_TRIGGER_PRICE:
            case MKT_PRICE_MARGIN_AND_TRIGGER_PRICE_AND_ACTIVATION_STATE: {
                validateEditedTriggerPrice();
                validateEditedMarketPriceMargin();
                break;
            }
            default:
                break;
        }
    }

    private void validateEditedActivationState() {
        if (!isZeroEditedFixedPriceString || !isZeroEditedMarketPriceMargin || !isZeroEditedTriggerPrice)
            throw new IllegalStateException(
                    format("programmer error: cannot change fixed price (%s), "
                                    + " mkt price margin (%s), or trigger price (%s) "
                                    + " in offer with id '%s' when only changing activation state",
                            editedPriceAsString,
                            editedMarketPriceMargin,
                            editedTriggerPrice,
                            currentlyOpenOffer.getId()));
    }

    private void validateEditedFixedPrice() {
        if (currentlyOpenOffer.getOffer().isUseMarketBasedPrice())
            log.info("Attempting to change mkt price margin based offer with id '{}' to fixed price offer.",
                    currentlyOpenOffer.getId());

        if (editedUseMarketBasedPrice)
            throw new IllegalStateException(
                    format("programmer error: cannot change fixed price (%s)"
                                    + " in mkt price based offer with id '%s'",
                            editedMarketPriceMargin,
                            currentlyOpenOffer.getId()));

        if (!isZeroEditedTriggerPrice)
            throw new IllegalStateException(
                    format("programmer error: cannot change trigger price (%s)"
                                    + " in offer with id '%s' when changing fixed price",
                            editedTriggerPrice,
                            currentlyOpenOffer.getId()));

    }

    private void validateEditedMarketPriceMargin() {
        if (!currentlyOpenOffer.getOffer().isUseMarketBasedPrice())
            log.info("Attempting to change fixed price offer with id '{}' to mkt price margin based offer.",
                    currentlyOpenOffer.getId());

        if (!editedUseMarketBasedPrice && !isZeroEditedTriggerPrice)
            throw new IllegalStateException(
                    format("programmer error: cannot set a trigger price (%s)"
                                    + " in fixed price offer with id '%s'",
                            editedTriggerPrice,
                            currentlyOpenOffer.getId()));

        if (!isZeroEditedFixedPriceString)
            throw new IllegalStateException(
                    format("programmer error: cannot set fixed price (%s)"
                                    + " in mkt price margin based offer with id '%s'",
                            editedPriceAsString,
                            currentlyOpenOffer.getId()));
    }

    private void validateEditedTriggerPrice() {
        if (editedTriggerPrice < 0)
            throw new IllegalStateException(
                    format("programmer error: cannot set trigger price to a negative value"
                                    + " in offer with id '%s'",
                            currentlyOpenOffer.getId()));
    }
}
