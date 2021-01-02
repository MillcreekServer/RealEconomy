package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IVisitStateProvider;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;

import java.util.Set;
import java.util.UUID;

public class VisitStateProviderModule extends AbstractModule {
    @Provides
    @Singleton
    IVisitStateProvider visitStateProvider(BusinessMediator mediator) {
        return new IVisitStateProvider() {
            @Override
            public Set<IBusiness> getUsingBusiness(UUID memberUuid) {
                return mediator.getUsingBusiness(memberUuid);
            }

            @Override
            public boolean isMember(IBusiness business, UUID memberUuid) {
                return mediator.isMember(business, memberUuid);
            }
        };
    }
}
