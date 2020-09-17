package io.github.wysohn.realeconomy.interfaces.business;

public interface IBusinessOwner {
    boolean addBusiness(IBusiness business);

    boolean removeBusiness(IBusiness business);

    boolean isOwnerOf(IBusiness business);

    boolean hasBusinessByType(Class<? extends IBusiness> type);
}
