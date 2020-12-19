package com.github.gpaddons.util.lang.replacement;

import com.github.gpaddons.util.lang.MessageReplacement;
import me.ryanhamshire.GriefPrevention.Claim;
import org.jetbrains.annotations.NotNull;

/**
 * MessageReplacement providing variables representing a {@link Claim}.
 *
 * <p>Supports $claimId, $area, $width (X) and $depth (Z) in addition to an
 * {@link OwnerReplacement}, minimum coordinate via a {@link LocationReplacement},
 * and maximum coordinate via a {@link LocationReplacement} with suffix set to "Max"
 * (i.e. $locXMax).
 */
public class ClaimReplacement implements MessageReplacement
{

    private final @NotNull Claim claim;
    private final @NotNull OwnerReplacement ownerReplacement;
    private final @NotNull LocationReplacement minReplacement;
    private final @NotNull LocationReplacement maxReplacement;

    public ClaimReplacement(@NotNull Claim claim)
    {
        this.claim = claim;
        this.ownerReplacement = new OwnerReplacement(claim.getOwnerID());
        this.minReplacement = new LocationReplacement(claim.getLesserBoundaryCorner());
        this.maxReplacement = new LocationReplacement("Max", claim.getGreaterBoundaryCorner());
    }

    @Override
    public @NotNull String replace(@NotNull String value)
    {
        value = ownerReplacement.replace(value);
        value = minReplacement.replace(value);
        value = maxReplacement.replace(value);

        value = value.replace("$claimId", String.valueOf(claim.getID()));
        value = value.replace("$area", String.valueOf(claim.getArea()));
        value = value.replace("$width", String.valueOf(claim.getWidth()));
        value = value.replace("$depth", String.valueOf(claim.getHeight()));

        return value;
    }

}
