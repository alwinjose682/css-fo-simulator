package io.alw.css.fosimulator.model.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("cash-message.definition")
public class CashMessageDefinitionProperties {
    private final int vdForwardDays;
    private final int vdBackwardDays;
    private final int numOfCfsForABackVdCf;
    private final int maxNumOfAmendments;

    @ConstructorBinding
    public CashMessageDefinitionProperties(int vdForwardDays, int vdBackwardDays, int cfsForBackvdCf, int maxAmendments) {
        if (vdBackwardDays == 0) {
            throw new RuntimeException("config param: vdBackwardDays, should not be zero");
        }
        this.vdForwardDays = Math.abs(vdForwardDays);
        this.vdBackwardDays = Math.abs(vdBackwardDays);
        this.numOfCfsForABackVdCf = Math.abs(cfsForBackvdCf);
        this.maxNumOfAmendments = maxAmendments;
    }

    public int vdForwardDays() {
        return vdForwardDays;
    }

    public int vdBackwardDays() {
        return vdBackwardDays;
    }

    public int numOfCfsForABackVdCf() {
        return numOfCfsForABackVdCf;
    }

    public int maxNumOfAmendments() {
        return maxNumOfAmendments;
    }
}
