package io.github.wysohn.realeconomy.manager.business.types;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.IVisitor;
import io.github.wysohn.realeconomy.interfaces.business.ITier;

import java.util.UUID;

public class MiningBusiness extends AbstractBusiness {
    private MiningBusiness() {
        super(null, null, null);
    }

    public MiningBusiness(UUID key,
                          UUID ownerUuid,
                          ITier tier) {
        super(key, ownerUuid, tier);
    }

    @Override
    public IMemento saveState() {
        return null;
    }

    @Override
    public void restoreState(IMemento iMemento) {

    }

    @Override
    public boolean addVisitor(IVisitor visitor) {
        return false;
    }

    @Override
    public boolean removeVisitor(IVisitor visitor) {
        return false;
    }
}
