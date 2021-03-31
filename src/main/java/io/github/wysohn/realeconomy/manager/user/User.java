package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.TradeMediator;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class User extends AbstractBankUser implements IBankOwner {
    @Inject
    private ManagerLanguage lang;
    @Inject
    private AssetListingManager listingManager;
    @Inject
    private ITaskSupervisor task;

    private User() {
        super(null);
    }

    public User(UUID key) {
        super(key);
    }

    @Override
    public void handleTransactionResult(TradeInfo info,
                                        OrderType type,
                                        TradeMediator.TradeResult result) {
        Validation.assertNotNull(type);

        String message = null;
        switch (result) {
            case INVALID_INFO:
                message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_InvalidInfo);
                break;
            case WITHDRAW_REFUSED:
                if (type == OrderType.BUY)
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_WithdrawFailAsBuyer);
                else
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_WithdrawFailAsSeller);
                break;
            case DEPOSIT_REFUSED:
                if (type == OrderType.BUY)
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_DepositFailAsBuyer);
                else
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_DepositFailAsSeller);
                break;
            case INSUFFICIENT_ASSETS:
                if (type == OrderType.BUY)
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_InsufficientAssetsBuyer);
                else
                    message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_InsufficientAssetsSeller);
                break;
            case OK:
                message = lang.parseFirst(this, RealEconomyLangs.DelayedMessage_Ok);
                break;
        }

        if (message != null) {
            String finalMessage = message;
            String typeStr = lang.parseFirst(this, type == OrderType.BUY ?
                    RealEconomyLangs.TradeResult_Buy : RealEconomyLangs.TradeResult_Sell);
            String listingStr = listingManager.get(info.getListingUuid())
                    .map(Reference::get)
                    .map(AssetListing::toString)
                    .orElse("N/A");

            int amountTraded = Math.min(info.getAmount(), info.getStock());
            lang.enqueueMessage(this, RealEconomyLangs.DelayedMessage_Format, (s, man) ->
                    man.addDate(new Date(System.currentTimeMillis()))
                            .addString(typeStr).addString(listingStr).addInteger(amountTraded)
                            .addString(finalMessage));
        }
    }

    @Override
    public int realizeAsset(Asset asset) {
        if (asset instanceof Item) {
            ItemStackSignature signature = (ItemStackSignature) asset.getSignature();
            return give(signature.getItemStack(), ((Item) asset).getAmount());
        } else {
            throw new RuntimeException("Not yet implemented: " + asset);
        }
    }

    @Override
    public String toString() {
        return Optional.ofNullable(getStringKey())
                .orElse("[?]");
    }
}
