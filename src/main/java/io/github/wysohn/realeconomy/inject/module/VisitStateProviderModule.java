package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IClaimHandler;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;

import java.util.Set;
import java.util.UUID;

public class VisitStateProviderModule extends AbstractModule {
    @Provides
    @Singleton
    IClaimHandler visitStateProvider(BusinessMediator mediator) {
        return new IClaimHandler() {
            @Override
            public Set<UUID> getUsingBusiness(UUID memberUuid) {
                return mediator.getUsingBusiness(memberUuid);
            }

            @Override
            public boolean isInBusiness(IBusiness business, UUID memberUuid) {
                return mediator.isMember(business, memberUuid);
            }

            @Override
            public UUID queryBusiness(SimpleLocation location) {
                return mediator.queryBusiness(location);
            }

            @Override
            public SimpleLocation getLocationOfBusiness(IBusiness business) {
                return mediator.getLocationOfBusiness(business);
            }

            @Override
            public boolean updateMapping(SimpleLocation location, IBusiness business) {
                return mediator.updateMapping(location, business);
            }

            @Override
            public boolean removeMapping(SimpleLocation location, IBusiness business) {
                return mediator.removeMapping(location, business);
            }
        };
    }
}
