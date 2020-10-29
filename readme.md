## GPClaimExpiration
A GriefPrevention addon for improved claim expiration.

## Settings
### Expiration Speed
Control the rate at which player evaluation occurs.
* `expiration.evaluation.rate`
  * `type`
    * Default: `PERCENT`
    * Type of check. May be `PERCENT` for a percentage of offline players per hour or `COUNT` for a fixed number of players per hour.
  * `value`
    * Default: `4.35`
    * Value used in conjunction with check type.
      For `PERCENT` this is the percentage of players checked per hour.
      For `COUNT` this is the number of players checked per hour.
* `expiration.evaluation.random`
  * Default: `false`
  * Whether check should be randomized during check run or not.
    Larger servers may want to use this option in conjunction with a fixed count per hour.
    Smaller servers are unlikely to have trouble checking all players before restarts.

### Claim Expiration
Control how long claims take to expire and what happens when they do.

* `expiration.days_per_area`
  * All keys are claim areas. All values are the number of days that a claim of that size or greater (until the next specified area) is protected.
  * Defaults:
    * `250000: -1`: If a claim's area is 250,000 blocks (500x500) or more it will not expire.
    * `10000: 90`: 10,000 (100x100)-249,999 block claims expire when the owner has been offline for 90 days.
    * `100: 60`: 100 (10x10)-10,000 block claims expire when the owner has been offline for 60 days.
    * `0: 30`: 0-99 block claims (new chest claims only with default GP config) are only protected for 30 days.
* `expiration.claim.commands`
  * Default: `[]`
  * Commands that will be run after claim deletion.
  * Placeholders: $playerName, $playerUUID, $claimId, $area, $world, $locX, $locY, $locZ, $locMaxX, $locMaxY, $locMaxZ

### Bypass Expiration
Settings for allowing players to bypass claim expiration limits:
* `expiration.bypass`
  * `claim_blocks`
    * Default: `10000`
    * Players with this many or more claim blocks will never have their claims expire. `-1` to disable.
  * `bonus_claim_blocks`
    * Default: `-1`
    * Players with this many or more bonus claim blocks will never have their claims expire. `-1` to disable.
  * `permissions`
    * Default: `[gpclaimexpiration.persist]`
    * Players with this permission assigned will never have their claims expire. Requires installation of Vault and a compatible permission system.

### Expire Pet Ownership
If a pet is not in a claim, it will be released when interacted with.  
If a pet is in a claim, it will be transferred to the owner if its owner does
not have permissions in the claim and meets the same inactivity requirements.

* `expiration.pet.days`
  * Default: `60`
  * Number of days that the pet's owner must have been inactive for.
* `expiration.pet.commands`
  * Default: `[]`
  * Commands that will be run after removing ownership from a pet.
  * Placeholders: $playerName, $playerUUID, $world, $locX, $locY, $locZ
