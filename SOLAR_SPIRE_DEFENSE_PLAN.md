# Solar Spire Defense System Implementation Plan

## Current Status
✅ Slowed down cleansing speed (50 blocks per 0.5 seconds)
✅ Added working_state animation transition after power-up
✅ Added spike damage system (4 damage in 3-block radius)

## Remaining Features to Implement

### 1. Health System with Boss Bar
**Requirements:**
- Solar Spire has 200 HP during working state
- Boss bar appears when working_state begins
- Boss bar disappears when cleansing completes or spire is destroyed
- Health stored in BlockEntity NBT for persistence

**Implementation:**
- Add health field to SolarSpireBlockEntity
- Create BossBar when transitioning to working state
- Update BossBar on damage
- Remove BossBar on completion/destruction

### 2. Make Cursed Entities Target the Spire
**Requirements:**
- Cursed mobs prioritize attacking the Solar Spire when it's active
- They should pathfind to the spire from up to 50 blocks away
- Attack damage: 5-10 HP per hit depending on mob type

**Implementation:**
- Create new AI Goal: `AttackSolarSpireGoal`
- Add to cursed entity AI during their initialization
- Goal should check for active spires in range
- Override target selection when spire is active

### 3. Spire Destruction Handling
**Requirements:**
- When health reaches 0, spire breaks
- Eye of Apophis drops
- Cleansing immediately stops
- Protection zone is removed
- Players get failure message

**Implementation:**
- Check health in damage handler
- Trigger break event when health <= 0
- Clean up all active operations
- Drop Eye item entity

### 4. Visual/Audio Feedback
**Requirements:**
- Different particle effects for spike damage
- Warning sounds when spire is low health (<50 HP)
- Screen shake effect when spire takes heavy damage
- Red tint on boss bar when critical (<25% health)

## Code Structure Changes Needed

### SolarSpireBlockEntity
```java
- int health = 200;
- BossBar bossBar;
- Set<UUID> trackingPlayers;
+ getHealth(), setHealth(), damage()
+ createBossBar(), updateBossBar(), removeBossBar()
```

### SolarSpireBlock
```java
+ handleSpireDamage(amount)
+ onSpireDestroyed()
+ getNearbyPlayers(range)
```

### Cursed Entity Classes
```java
+ AttackSolarSpireGoal extends Goal
+ Check for active spires
+ Pathfind and attack logic
```

## Testing Checklist
- [ ] Spawn Solar Spire and activate with Eye
- [ ] Verify boss bar appears after power-up
- [ ] Spawn cursed mobs - they should attack spire
- [ ] Test spike damage on approaching mobs
- [ ] Let mobs destroy spire - verify Eye drops
- [ ] Test server restart - health persists
- [ ] Test multiplayer - boss bar visible to all nearby

## Performance Considerations
- Limit boss bar updates to every 5 ticks
- Cache nearby player list for boss bar
- Optimize entity search for spike damage
- Use squared distance checks where possible