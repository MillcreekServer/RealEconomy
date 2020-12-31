package io.github.wysohn.realeconomy.interfaces.business;

public interface IBusinessOwner {
    boolean addBusiness(IUpgradable business);

    boolean removeBusiness(IUpgradable business);

    boolean isOwnerOf(IUpgradable business);

    boolean hasBusinessByType(Class<? extends IUpgradable> type);
}
