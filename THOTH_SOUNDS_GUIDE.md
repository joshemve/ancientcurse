# Thoth Sound Design Guide

This guide provides production-level recommendations for Thoth's sound effects. All sounds use vanilla fallbacks but can be easily replaced with custom audio.

## Sound File Structure

Place your custom sound files here:
```
src/main/resources/assets/ancientcurse/sounds/thoth/
├── ambient.ogg
├── attack_magic_ball.ogg
├── attack_melee.ogg
├── attack_scroll_blast.ogg
├── attack_time_bend.ogg
├── spawn.ogg
├── death.ogg
├── hurt1.ogg
├── hurt2.ogg
├── hurt3.ogg
└── summon.ogg
```

## Sound Specifications

### Technical Requirements
- **Format**: OGG Vorbis (`.ogg`)
- **Sample Rate**: 44100 Hz (recommended) or 48000 Hz
- **Bit Depth**: 16-bit minimum
- **Channels**: Mono (preferred for 3D positioning) or Stereo
- **Compression**: Quality 5-7 for balance between size and quality

---

## Individual Sound Recommendations

### 1. **thoth_ambient.ogg**
**When**: Plays randomly when Thoth is idle
**Duration**: 1-3 seconds
**Volume**: 0.8 (slightly quieter than attacks)
**Pitch**: 1.0

**Sound Design Suggestions:**
- Deep, mystical Egyptian chant or whisper
- Ancient language vocalizations
- Subtle papyrus rustling with breathy voice
- Low-pitched magical hum with ethereal quality

**Vanilla Fallback:** `entity.witch.ambient`

**Inspiration:** Think "wise ancient god muttering incantations" - mysterious but not threatening when peaceful.

---

### 2. **attack_magic_ball.ogg**
**When**: Projectile launches during magic ball attack (frame 23)
**Duration**: 0.5-1.5 seconds
**Volume**: 1.0
**Pitch**: 1.0

**Sound Design Suggestions:**
- Magical whoosh with crystalline shimmer
- Energy charging then releasing
- Purple/mystical energy buildup + release
- Think: *Whooom-FWOOSH*

**Vanilla Fallback:** `entity.blaze.shoot`

**Layering Ideas:**
- Base: Deep magical hum
- Mid: Whooshing air movement
- High: Sparkly magical particles

---

### 3. **attack_melee.ogg** ⭐ NEW
**When**: Melee strike hits target (frame 23)
**Duration**: 0.3-0.8 seconds
**Volume**: 1.0
**Pitch**: 0.9

**Sound Design Suggestions:**
- Heavy magical staff whoosh + impact
- Energy-infused physical strike
- Think: Staff swing with magical aftershock
- **Not** a simple "thud" - should feel divine/powerful

**Vanilla Fallback:** `entity.player.attack.strong`

**Layering Ideas:**
- Initial: Powerful whoosh (staff swing)
- Impact: Deep thump with magical crackle
- Tail: Brief magical reverberation

---

### 4. **attack_scroll_blast.ogg**
**When**: Scroll blast explosion (frame 80 of attack_2 animation)
**Duration**: 1.5-3 seconds
**Volume**: 1.2 (louder - AoE attack)
**Pitch**: 1.0

**Sound Design Suggestions:**
- Ancient tome opening + explosive magical release
- Papyrus fluttering + arcane explosion
- Think: Book slam open → massive energy wave
- Should feel like forbidden knowledge unleashed

**Vanilla Fallback:** `entity.generic.explode`

**Phases:**
1. **Windup (0.0-0.5s)**: Papyrus rustling, building magical energy
2. **Release (0.5-1.0s)**: Explosive magical burst
3. **Aftermath (1.0-3.0s)**: Echoing mystical reverb, fading energy

---

### 5. **attack_time_bend.ogg**
**When**: Time magic activation (frame 51)
**Duration**: 2-4 seconds
**Volume**: 1.0
**Pitch**: 0.8 (lower pitch for time distortion)

**Sound Design Suggestions:**
- Reality warping effect
- Clock/time sounds reversed/distorted
- Deep, reverberating cosmic energy
- Doppler effect (pitch shift up then down)
- Think: Time slowing down audibly

**Vanilla Fallback:** `block.portal.ambient`

**Audio Processing:**
- Heavy reverb with long decay
- Pitched-down magical elements
- Subtle "ticking" or temporal elements
- Layer distorted/reversed sounds

**Mood:** Unsettling, powerful, reality-bending

---

### 6. **spawn.ogg**
**When**: Thoth first appears in the world
**Duration**: 2-4 seconds
**Volume**: 1.5 (LOUD - dramatic entrance)
**Pitch**: 0.9

**Sound Design Suggestions:**
- Epic boss entrance
- Divine manifestation
- Portal opening with Egyptian mysticism
- Think: "A GOD has arrived"

**Vanilla Fallback:** `entity.ender_dragon.growl`

**Structure:**
1. **Intro (0.0-0.5s)**: Reality tearing/portal opening
2. **Peak (0.5-2.0s)**: Massive magical surge, divine chant
3. **Settle (2.0-4.0s)**: Energy settling, establishing presence

**Should feel:** Dramatic, intimidating, awe-inspiring

---

### 7. **death.ogg**
**When**: Thoth dies
**Duration**: 3-5 seconds
**Volume**: 1.3
**Pitch**: 0.8 (deeper/sadder)

**Sound Design Suggestions:**
- Defeated divine being
- Energy dissipating/fading away
- Last ancient words/chant fading out
- NOT comedic - should be epic and sorrowful

**Vanilla Fallback:** `entity.witch.death`

**Emotional Arc:**
1. **Impact (0.0-0.5s)**: Final hit received, pain
2. **Collapse (0.5-2.0s)**: Weakness, divine power fading
3. **Fade (2.0-5.0s)**: Essence dissipating, whispers fading to silence

---

### 8. **hurt1.ogg, hurt2.ogg, hurt3.ogg**
**When**: Thoth takes damage (random selection)
**Duration**: 0.3-0.8 seconds each
**Volume**: 0.9
**Pitch**: 1.0

**Sound Design Suggestions:**
- Sharp magical grunt/yelp
- Divine pain vocalization
- Mystical energy disruption
- Mix of voice + magic

**Vanilla Fallback:** `entity.witch.hurt`

**Variety Tips:**
- **hurt1**: Higher pitched, surprised
- **hurt2**: Mid-range, angry
- **hurt3**: Lower, more pained

**Should convey:** "I'm powerful but that actually hurt" - not overly dramatic but acknowledges damage

---

### 9. **summon.ogg**
**When**: Summoning scarab beetles/entities (frame 40)
**Duration**: 1.5-3 seconds
**Volume**: 1.2
**Pitch**: 0.9

**Sound Design Suggestions:**
- Necromantic summoning ritual
- Ancient Egyptian resurrection magic
- Think: Calling forth servants from beyond
- Insect chittering building up in the background

**Vanilla Fallback:** `entity.evoker.cast_spell`

**Layering:**
- Base: Deep chanting/incantation
- Mid: Magical energy swirling
- High: Faint insect sounds emerging
- Peak: Sudden surge as entities appear

---

## Production Sound Design Tips

### Audio Processing Chain
1. **EQ**: Cut low-end mud (<80Hz), boost presence (2-5kHz)
2. **Compression**: Light compression for consistency
3. **Reverb**:
   - Ambient/hurt: Small room (0.5-1s decay)
   - Attacks: Medium hall (1-2s decay)
   - Time bend/spawn/death: Large space (2-4s decay)
4. **Pitch Shifting**: Use for variety without re-recording

### Mixing Levels
- **Ambient**: -6dB to -3dB
- **Attacks**: 0dB (reference level)
- **Spawn/Death**: +2dB to +3dB (epic moments)
- **Hurt**: -3dB to -1dB

### Spatial Design
- **Mono** sources work best for in-game 3D positioning
- **Stereo** width should be narrow if used (<30% width)
- Trust Minecraft's 3D audio system for positioning

---

## Subtitle Localization

Add to `src/main/resources/assets/ancientcurse/lang/en_us.json`:

```json
{
  "subtitles.ancientcurse.thoth_ambient": "Thoth whispers ancient words",
  "subtitles.ancientcurse.thoth_attack_magic_ball": "Thoth casts magic ball",
  "subtitles.ancientcurse.thoth_attack_melee": "Thoth strikes with staff",
  "subtitles.ancientcurse.thoth_attack_scroll_blast": "Thoth unleashes forbidden knowledge",
  "subtitles.ancientcurse.thoth_attack_time_bend": "Time bends to Thoth's will",
  "subtitles.ancientcurse.thoth_spawn": "Thoth manifests",
  "subtitles.ancientcurse.thoth_death": "Thoth fades from existence",
  "subtitles.ancientcurse.thoth_hurt": "Thoth grunts in pain",
  "subtitles.ancientcurse.thoth_summon": "Thoth summons servants"
}
```

---

## Sound Timing Reference

| Sound | Triggered At | Animation Sync |
|-------|-------------|----------------|
| `ambient` | Random idle | Passive |
| `attack_magic_ball` | Start of attack | Attack initiated |
| `attack_magic_ball` (2nd) | Frame 23/45 | Projectile launches |
| `attack_melee` | Frame 23/45 | Impact frame |
| `attack_scroll_blast` | Frame 80/110 | Explosion peak |
| `attack_time_bend` | Frame 51/103 | Time pulse |
| `spawn` | Entity spawn | Spawn animation start |
| `death` | Health = 0 | Death animation |
| `hurt` | Damage received | Immediate |
| `summon` | Frame 40/60 | Entities appear |

---

## Royalty-Free Sound Resources

### Recommended Sources
1. **Freesound.org** - CC0/CC-BY sounds
2. **ZapSplat** - Free with attribution
3. **BBC Sound Effects** - Public domain
4. **Incompetech** - Royalty-free with credit

### Search Terms
- "ancient chant", "mystical", "magical whoosh"
- "energy beam", "arcane explosion"
- "time warp", "reality distortion"
- "papyrus", "scroll", "ancient"
- "divine", "godly", "ethereal"

---

## Testing Your Sounds

1. Place `.ogg` files in `sounds/thoth/` folder
2. Run `./gradlew build`
3. Test in-game with `/summon ancientcurse:thoth`
4. Adjust volume/pitch in `sounds.json` as needed
5. Volume range: 0.0 (silent) to 2.0 (very loud)
6. Pitch range: 0.5 (deep) to 2.0 (high)

---

## Current Vanilla Fallbacks

All sounds have fallbacks so the mod works without custom audio:
- Missing sounds will use vanilla Minecraft sounds
- No crashes or errors - graceful degradation
- Professional user experience even without custom audio

**Your mod is production-ready with or without custom sounds!** ✅
