package com.sas.lostandfound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmojiData {

    public static class EmojiItem {
        public final String emoji;
        public final String name;
        public final String category;
        public final List<String> keywords;

        public EmojiItem(String emoji, String name, String category, String... keywords) {
            this.emoji = emoji;
            this.name = name;
            this.category = category;
            this.keywords = Arrays.asList(keywords);
        }

        public boolean matches(String query) {
            if (query == null || query.isEmpty()) return true;
            String lower = query.toLowerCase().trim();
            if (name.toLowerCase().contains(lower)) return true;
            if (category.toLowerCase().contains(lower)) return true;
            for (String kw : keywords) {
                if (kw.toLowerCase().contains(lower)) return true;
            }
            return false;
        }
    }

    public static final List<String> CATEGORIES = Arrays.asList(
        "😀 Smileys & Emotion",
        "👋 People & Body",
        "🐶 Animals & Nature",
        "🍔 Food & Drink",
        "🌍 Travel & Places",
        "⚽ Activities",
        "📦 Objects",
        "🔣 Symbols",
        "🏳️ Flags",
        "👨‍👩‍👧‍👦 Component"
    );

    private static List<EmojiItem> allEmojis = null;

    public static synchronized List<EmojiItem> getAllEmojis() {
        if (allEmojis != null) {
            return allEmojis;
        }

        List<EmojiItem> list = new ArrayList<>();

        // 1. 😀 Smileys & Emotion
        list.add(new EmojiItem("👍", "thumbs up", "😀 Smileys & Emotion", "like", "yes", "ok", "good", "agree"));
        list.add(new EmojiItem("❤️", "red heart", "😀 Smileys & Emotion", "love", "like", "heart", "red"));
        list.add(new EmojiItem("😂", "laughing crying", "😀 Smileys & Emotion", "haha", "laugh", "happy", "tears", "joy"));
        list.add(new EmojiItem("😮", "wow surprise", "😀 Smileys & Emotion", "wow", "surprised", "shocked", "hushed"));
        list.add(new EmojiItem("😢", "crying sad", "😀 Smileys & Emotion", "cry", "sad", "tears", "tear"));
        list.add(new EmojiItem("😡", "angry face", "😀 Smileys & Emotion", "mad", "rage", "furious", "angry"));
        list.add(new EmojiItem("😀", "grinning face", "😀 Smileys & Emotion", "smile", "happy", "joy", "grin"));
        list.add(new EmojiItem("😃", "happy face", "😀 Smileys & Emotion", "smile", "happy", "joy"));
        list.add(new EmojiItem("😄", "smiling squinting", "😀 Smileys & Emotion", "smile", "happy", "joy"));
        list.add(new EmojiItem("😁", "beaming smile", "😀 Smileys & Emotion", "smile", "happy", "grin"));
        list.add(new EmojiItem("😆", "laughing squinting", "😀 Smileys & Emotion", "laugh", "happy", "haha"));
        list.add(new EmojiItem("😅", "sweat smile", "😀 Smileys & Emotion", "smile", "sweat", "relief"));
        list.add(new EmojiItem("🤣", "rolling on floor laughing", "😀 Smileys & Emotion", "laugh", "funny", "haha", "rofl"));
        list.add(new EmojiItem("😊", "smiling eyes blush", "😀 Smileys & Emotion", "smile", "happy", "blush"));
        list.add(new EmojiItem("😇", "halo smile", "😀 Smileys & Emotion", "angel", "halo", "innocent"));
        list.add(new EmojiItem("🙂", "slight smile", "😀 Smileys & Emotion", "smile", "okay"));
        list.add(new EmojiItem("🙃", "upside down smile", "😀 Smileys & Emotion", "flip", "silly"));
        list.add(new EmojiItem("😉", "winking face", "😀 Smileys & Emotion", "wink", "joke"));
        list.add(new EmojiItem("😌", "relieved face", "😀 Smileys & Emotion", "relieved", "calm", "peace"));
        list.add(new EmojiItem("😍", "heart eyes", "😀 Smileys & Emotion", "love", "heart", "crush"));
        list.add(new EmojiItem("🥰", "hearts face", "😀 Smileys & Emotion", "love", "hearts", "blush"));
        list.add(new EmojiItem("😘", "blowing kiss", "😀 Smileys & Emotion", "kiss", "love", "heart"));
        list.add(new EmojiItem("😗", "kissing face", "😀 Smileys & Emotion", "kiss", "lips"));
        list.add(new EmojiItem("😙", "kissing smiling eyes", "😀 Smileys & Emotion", "kiss"));
        list.add(new EmojiItem("😚", "kissing closed eyes", "😀 Smileys & Emotion", "kiss"));
        list.add(new EmojiItem("😋", "delicious food", "😀 Smileys & Emotion", "yummy", "taste", "hungry"));
        list.add(new EmojiItem("😛", "tongue face", "😀 Smileys & Emotion", "tongue", "silly"));
        list.add(new EmojiItem("😜", "wink tongue", "😀 Smileys & Emotion", "tongue", "wink", "silly"));
        list.add(new EmojiItem("🤪", "crazy face", "😀 Smileys & Emotion", "crazy", "silly", "goofy"));
        list.add(new EmojiItem("🤨", "raised eyebrow", "😀 Smileys & Emotion", "skeptical", "suspicious", "doubt"));
        list.add(new EmojiItem("🧐", "monocle face", "😀 Smileys & Emotion", "smart", "glasses", "inspect"));
        list.add(new EmojiItem("🤓", "nerd face", "😀 Smileys & Emotion", "nerd", "smart", "geek", "glasses"));
        list.add(new EmojiItem("😎", "cool sunglasses", "😀 Smileys & Emotion", "cool", "sunglasses", "chill"));
        list.add(new EmojiItem("🥸", "disguised face", "😀 Smileys & Emotion", "disguise", "mask", "glasses"));
        list.add(new EmojiItem("🤩", "star struck", "😀 Smileys & Emotion", "star", "eyes", "wow"));
        list.add(new EmojiItem("🥳", "party face", "😀 Smileys & Emotion", "party", "celebrate", "birthday"));
        list.add(new EmojiItem("😏", "smirking face", "😀 Smileys & Emotion", "smirk", "sly", "cheeky"));
        list.add(new EmojiItem("😒", "unamused face", "😀 Smileys & Emotion", "meh", "bored", "unamused"));
        list.add(new EmojiItem("😞", "disappointed", "😀 Smileys & Emotion", "sad", "disappointed"));
        list.add(new EmojiItem("😔", "pensive face", "😀 Smileys & Emotion", "sad", "pensive"));
        list.add(new EmojiItem("😟", "worried face", "😀 Smileys & Emotion", "worry", "anxious"));
        list.add(new EmojiItem("😕", "confused face", "😀 Smileys & Emotion", "confused", "puzzled"));
        list.add(new EmojiItem("🙁", "slight frown", "😀 Smileys & Emotion", "sad", "frown"));
        list.add(new EmojiItem("☹️", "frowning face", "😀 Smileys & Emotion", "sad", "frown"));
        list.add(new EmojiItem("😣", "persevering face", "😀 Smileys & Emotion", "struggle", "sad"));
        list.add(new EmojiItem("😖", "confounded face", "😀 Smileys & Emotion", "confounded", "upset"));
        list.add(new EmojiItem("😫", "tired face", "😀 Smileys & Emotion", "tired", "sleepy"));
        list.add(new EmojiItem("😩", "weary face", "😀 Smileys & Emotion", "tired", "weary", "sad"));
        list.add(new EmojiItem("🥺", "pleading eyes", "😀 Smileys & Emotion", "please", "pleading", "beg", "sad"));
        list.add(new EmojiItem("😭", "loudly crying", "😀 Smileys & Emotion", "cry", "sad", "sobbing", "tears"));
        list.add(new EmojiItem("😤", "steam nose", "😀 Smileys & Emotion", "angry", "steam", "triumph"));
        list.add(new EmojiItem("🤬", "symbols on mouth", "😀 Smileys & Emotion", "angry", "cuss", "swear"));
        list.add(new EmojiItem("🤯", "exploding head", "😀 Smileys & Emotion", "mindblown", "shocked", "wow"));
        list.add(new EmojiItem("😳", "flushed face", "😀 Smileys & Emotion", "blush", "embarrassed", "shocked"));
        list.add(new EmojiItem("🥵", "hot face", "😀 Smileys & Emotion", "hot", "sunburn", "sweat", "warm"));
        list.add(new EmojiItem("🥶", "cold face", "😀 Smileys & Emotion", "cold", "freeze", "ice"));
        list.add(new EmojiItem("😱", "screaming fear", "😀 Smileys & Emotion", "fear", "scared", "scream", "shock"));
        list.add(new EmojiItem("😨", "fearful face", "😀 Smileys & Emotion", "fear", "scared"));
        list.add(new EmojiItem("😰", "anxious sweat", "😀 Smileys & Emotion", "sweat", "anxious", "scared"));
        list.add(new EmojiItem("😥", "sad relieved", "😀 Smileys & Emotion", "sad", "sweat", "relieved"));
        list.add(new EmojiItem("😓", "cold sweat", "😀 Smileys & Emotion", "sweat", "sad", "anxious"));
        list.add(new EmojiItem("🫣", "peeking face", "😀 Smileys & Emotion", "peek", "scared", "hide"));
        list.add(new EmojiItem("🤭", "hand over mouth", "😀 Smileys & Emotion", "giggle", "laugh", "secret"));
        list.add(new EmojiItem("🫢", "open mouth eyes", "😀 Smileys & Emotion", "gasp", "surprise", "shock"));
        list.add(new EmojiItem("🫡", "saluting face", "😀 Smileys & Emotion", "salute", "respect", "yes"));
        list.add(new EmojiItem("🤔", "thinking face", "😀 Smileys & Emotion", "think", "wonder", "ponder"));
        list.add(new EmojiItem("🤫", "shushing face", "😀 Smileys & Emotion", "shush", "quiet", "silent"));
        list.add(new EmojiItem("🫠", "melting face", "😀 Smileys & Emotion", "melt", "hot", "sarcasm"));
        list.add(new EmojiItem("🤥", "lying face", "😀 Smileys & Emotion", "lie", "pinocchio", "untrust"));
        list.add(new EmojiItem("😶", "no mouth", "😀 Smileys & Emotion", "silent", "speechless"));
        list.add(new EmojiItem("🫥", "dotted line face", "😀 Smileys & Emotion", "invisible", "fade", "hide"));
        list.add(new EmojiItem("😐", "neutral face", "😀 Smileys & Emotion", "meh", "neutral", "indifferent"));
        list.add(new EmojiItem("😑", "expressionless", "😀 Smileys & Emotion", "meh", "indifferent"));
        list.add(new EmojiItem("😬", "grimacing face", "😀 Smileys & Emotion", "grimace", "nervous", "awkward"));
        list.add(new EmojiItem("🙄", "rolling eyes", "😀 Smileys & Emotion", "eyeroll", "bored", "sarcasm"));
        list.add(new EmojiItem("😯", "hushed face", "😀 Smileys & Emotion", "surprise", "hushed"));
        list.add(new EmojiItem("😦", "frowning open mouth", "😀 Smileys & Emotion", "sad", "surprise"));
        list.add(new EmojiItem("😧", "anguished face", "😀 Smileys & Emotion", "sad", "shock"));
        list.add(new EmojiItem("😲", "astonished face", "😀 Smileys & Emotion", "surprise", "astonish"));
        list.add(new EmojiItem("🥱", "yawning face", "😀 Smileys & Emotion", "yawn", "sleepy", "tired"));
        list.add(new EmojiItem("😴", "sleeping face", "😀 Smileys & Emotion", "sleep", "zzz", "tired"));
        list.add(new EmojiItem("🤤", "drooling face", "😀 Smileys & Emotion", "drool", "sleepy", "hungry"));
        list.add(new EmojiItem("😪", "sleepy face", "😀 Smileys & Emotion", "sleepy", "tired", "bubble"));
        list.add(new EmojiItem("😵", "dizzy face", "😀 Smileys & Emotion", "dizzy", "dead"));
        list.add(new EmojiItem("😵‍💫", "spiral eyes", "😀 Smileys & Emotion", "dizzy", "spiral", "confused"));
        list.add(new EmojiItem("🤐", "zipper mouth", "😀 Smileys & Emotion", "zip", "secret", "silent"));
        list.add(new EmojiItem("🥴", "woozy face", "😀 Smileys & Emotion", "woozy", "drunk", "dizzy"));
        list.add(new EmojiItem("🤢", "nauseated face", "😀 Smileys & Emotion", "sick", "nausea", "gross"));
        list.add(new EmojiItem("🤮", "vomiting face", "😀 Smileys & Emotion", "sick", "puke", "vomit"));
        list.add(new EmojiItem("🤧", "sneezing face", "😀 Smileys & Emotion", "sick", "cold", "allergy"));
        list.add(new EmojiItem("😷", "medical mask", "😀 Smileys & Emotion", "sick", "mask", "doctor"));
        list.add(new EmojiItem("🤒", "thermometer face", "😀 Smileys & Emotion", "sick", "fever", "hot"));
        list.add(new EmojiItem("🤕", "head bandage", "😀 Smileys & Emotion", "sick", "hurt", "injured"));
        list.add(new EmojiItem("🤡", "clown face", "😀 Smileys & Emotion", "clown", "circus", "funny"));
        list.add(new EmojiItem("👹", "ogre", "😀 Smileys & Emotion", "monster", "ogre", "japanese", "spooky"));
        list.add(new EmojiItem("👺", "goblin", "😀 Smileys & Emotion", "monster", "goblin", "japanese", "spooky"));
        list.add(new EmojiItem("☠️", "skull and crossbones", "😀 Smileys & Emotion", "skull", "danger", "poison", "pirate"));
        list.add(new EmojiItem("👽", "alien", "😀 Smileys & Emotion", "alien", "space", "ufo"));
        list.add(new EmojiItem("👾", "alien monster", "😀 Smileys & Emotion", "alien", "game", "retro", "pixel"));
        list.add(new EmojiItem("🤖", "robot", "😀 Smileys & Emotion", "robot", "tech", "bot"));
        list.add(new EmojiItem("😺", "grinning cat", "😀 Smileys & Emotion", "cat", "happy", "smile"));
        list.add(new EmojiItem("😸", "beaming cat", "😀 Smileys & Emotion", "cat", "happy", "grin"));
        list.add(new EmojiItem("😹", "joy cat", "😀 Smileys & Emotion", "cat", "happy", "haha", "crying"));
        list.add(new EmojiItem("😻", "heart eyes cat", "😀 Smileys & Emotion", "cat", "love", "heart"));
        list.add(new EmojiItem("😼", "wry smile cat", "😀 Smileys & Emotion", "cat", "smirk"));
        list.add(new EmojiItem("😽", "kissing cat", "😀 Smileys & Emotion", "cat", "kiss"));
        list.add(new EmojiItem("🙀", "weary cat", "😀 Smileys & Emotion", "cat", "scared", "surprise"));
        list.add(new EmojiItem("😿", "crying cat", "😀 Smileys & Emotion", "cat", "sad", "cry"));
        list.add(new EmojiItem("😾", "pouting cat", "😀 Smileys & Emotion", "cat", "angry"));

        // 2. 👋 People & Body
        list.add(new EmojiItem("👋", "waving hand", "👋 People & Body", "wave", "hello", "hi", "bye"));
        list.add(new EmojiItem("🤚", "raised back of hand", "👋 People & Body", "hand", "raised"));
        list.add(new EmojiItem("🖐️", "hand fingers splayed", "👋 People & Body", "hand", "fingers"));
        list.add(new EmojiItem("✋", "raised hand", "👋 People & Body", "hand", "stop", "highfive"));
        list.add(new EmojiItem("🖖", "vulcan salute", "👋 People & Body", "vulcan", "salute", "spock"));
        list.add(new EmojiItem("👌", "ok hand", "👋 People & Body", "ok", "fine", "good"));
        list.add(new EmojiItem("🤌", "pinched fingers", "👋 People & Body", "italian", "pinched"));
        list.add(new EmojiItem("🤏", "pinching hand", "👋 People & Body", "pinch", "small"));
        list.add(new EmojiItem("✌️", "victory hand", "👋 People & Body", "peace", "victory", "two"));
        list.add(new EmojiItem("🤞", "crossed fingers", "👋 People & Body", "luck", "cross", "hope"));
        list.add(new EmojiItem("🤟", "love you gesture", "👋 People & Body", "love", "rock", "gesture"));
        list.add(new EmojiItem("🤘", "sign of horns", "👋 People & Body", "rock", "horns", "metal"));
        list.add(new EmojiItem("🤙", "call me hand", "👋 People & Body", "call", "phone", "hand"));
        list.add(new EmojiItem("👈", "backhand index pointing left", "👋 People & Body", "point", "left"));
        list.add(new EmojiItem("👉", "backhand index pointing right", "👋 People & Body", "point", "right"));
        list.add(new EmojiItem("👆", "backhand index pointing up", "👋 People & Body", "point", "up"));
        list.add(new EmojiItem("🖕", "middle finger", "👋 People & Body", "finger", "middle", "rude"));
        list.add(new EmojiItem("👇", "backhand index pointing down", "👋 People & Body", "point", "down"));
        list.add(new EmojiItem("☝️", "index pointing up", "👋 People & Body", "point", "up"));
        list.add(new EmojiItem("👎", "thumbs down", "👋 People & Body", "dislike", "no", "bad"));
        list.add(new EmojiItem("✊", "raised fist", "👋 People & Body", "fist", "power"));
        list.add(new EmojiItem("👊", "oncoming fist", "👋 People & Body", "fist", "punch", "bump"));
        list.add(new EmojiItem("🤛", "left-facing fist", "👋 People & Body", "fist", "punch"));
        list.add(new EmojiItem("🤜", "right-facing fist", "👋 People & Body", "fist", "punch"));
        list.add(new EmojiItem("🙌", "raising hands", "👋 People & Body", "hooray", "hands", "celebrate"));
        list.add(new EmojiItem("👐", "open hands", "👋 People & Body", "hands", "open"));
        list.add(new EmojiItem("🤲", "palms up together", "👋 People & Body", "pray", "book", "palms"));
        list.add(new EmojiItem("🤝", "handshake", "👋 People & Body", "meet", "agree", "shake"));
        list.add(new EmojiItem("🙏", "folded hands", "👋 People & Body", "please", "pray", "thanks", "hope"));
        list.add(new EmojiItem("✍️", "writing hand", "👋 People & Body", "write", "pencil"));
        list.add(new EmojiItem("💅", "nail polish", "👋 People & Body", "nail", "polish", "cosmetic", "glam"));
        list.add(new EmojiItem("🤳", "selfie", "👋 People & Body", "selfie", "phone", "camera"));
        list.add(new EmojiItem("💪", "flexed biceps", "👋 People & Body", "muscle", "strength", "power", "strong"));
        list.add(new EmojiItem("🦾", "mechanical arm", "👋 People & Body", "arm", "robot", "mechanical"));
        list.add(new EmojiItem("🦿", "mechanical leg", "👋 People & Body", "leg", "robot", "mechanical"));
        list.add(new EmojiItem("🦵", "leg", "👋 People & Body", "leg", "limb"));
        list.add(new EmojiItem("🦶", "foot", "👋 People & Body", "foot", "limb"));
        list.add(new EmojiItem("👂", "ear", "👋 People & Body", "ear", "hear", "listen"));
        list.add(new EmojiItem("🦻", "ear with hearing aid", "👋 People & Body", "ear", "hearingaid"));
        list.add(new EmojiItem("👃", "nose", "👋 People & Body", "nose", "smell"));
        list.add(new EmojiItem("🧠", "brain", "👋 People & Body", "brain", "smart", "mind"));
        list.add(new EmojiItem("🫀", "anatomical heart", "👋 People & Body", "heart", "anatomical", "organ"));
        list.add(new EmojiItem("🫁", "lungs", "👋 People & Body", "lungs", "organ", "breathe"));
        list.add(new EmojiItem("🦷", "tooth", "👋 People & Body", "tooth", "dentist"));
        list.add(new EmojiItem("🦴", "bone", "👋 People & Body", "bone", "dog", "skeleton"));
        list.add(new EmojiItem("👀", "eyes", "👋 People & Body", "eyes", "look", "see"));
        list.add(new EmojiItem("👁️", "eye", "👋 People & Body", "eye", "look", "see"));
        list.add(new EmojiItem("👅", "tongue", "👋 People & Body", "tongue", "taste"));
        list.add(new EmojiItem("👄", "mouth", "👋 People & Body", "mouth", "lips", "kiss"));
        list.add(new EmojiItem("🫂", "people hugging", "👋 People & Body", "hug", "people", "love"));

        // 3. 🐶 Animals & Nature
        list.add(new EmojiItem("🐶", "dog face", "🐶 Animals & Nature", "dog", "puppy", "pet", "animal"));
        list.add(new EmojiItem("🐱", "cat face", "🐶 Animals & Nature", "cat", "kitty", "pet", "animal"));
        list.add(new EmojiItem("🐭", "mouse face", "🐶 Animals & Nature", "mouse", "pet", "animal"));
        list.add(new EmojiItem("🐹", "hamster", "🐶 Animals & Nature", "hamster", "pet", "animal"));
        list.add(new EmojiItem("🐰", "rabbit face", "🐶 Animals & Nature", "rabbit", "bunny", "animal"));
        list.add(new EmojiItem("🦊", "fox", "🐶 Animals & Nature", "fox", "animal", "wild"));
        list.add(new EmojiItem("🐻", "bear", "🐶 Animals & Nature", "bear", "animal", "wild"));
        list.add(new EmojiItem("🐼", "panda", "🐶 Animals & Nature", "panda", "animal"));
        list.add(new EmojiItem("🐨", "koala", "🐶 Animals & Nature", "koala", "animal"));
        list.add(new EmojiItem("🐯", "tiger face", "🐶 Animals & Nature", "tiger", "animal"));
        list.add(new EmojiItem("🦁", "lion", "🐶 Animals & Nature", "lion", "animal"));
        list.add(new EmojiItem("🐮", "cow face", "🐶 Animals & Nature", "cow", "animal", "farm"));
        list.add(new EmojiItem("🐷", "pig face", "🐶 Animals & Nature", "pig", "animal", "farm"));
        list.add(new EmojiItem("🐽", "pig nose", "🐶 Animals & Nature", "pig", "nose"));
        list.add(new EmojiItem("🐸", "frog", "🐶 Animals & Nature", "frog", "animal", "green"));
        list.add(new EmojiItem("🐵", "monkey face", "🐶 Animals & Nature", "monkey", "animal"));
        list.add(new EmojiItem("🙈", "see-no-evil monkey", "🐶 Animals & Nature", "monkey", "blind", "hide"));
        list.add(new EmojiItem("🙉", "hear-no-evil monkey", "🐶 Animals & Nature", "monkey", "deaf"));
        list.add(new EmojiItem("🙊", "speak-no-evil monkey", "🐶 Animals & Nature", "monkey", "mute", "quiet"));
        list.add(new EmojiItem("🐒", "monkey", "🐶 Animals & Nature", "monkey", "animal"));
        list.add(new EmojiItem("🐔", "chicken", "🐶 Animals & Nature", "chicken", "bird", "farm"));
        list.add(new EmojiItem("🐧", "penguin", "🐶 Animals & Nature", "penguin", "bird", "ice"));
        list.add(new EmojiItem("🦅", "eagle", "🐶 Animals & Nature", "eagle", "bird"));
        list.add(new EmojiItem("🦉", "owl", "🐶 Animals & Nature", "owl", "bird", "night"));
        list.add(new EmojiItem("🐝", "honeybee", "🐶 Animals & Nature", "bee", "bug", "insect"));
        list.add(new EmojiItem("🐛", "caterpillar", "🐶 Animals & Nature", "bug", "worm"));
        list.add(new EmojiItem("🦋", "butterfly", "🐶 Animals & Nature", "butterfly", "insect"));
        list.add(new EmojiItem("🐌", "snail", "🐶 Animals & Nature", "snail", "bug", "slow"));
        list.add(new EmojiItem("🐞", "ladybeetle", "🐶 Animals & Nature", "bug", "ladybug"));
        list.add(new EmojiItem("🐜", "ant", "🐶 Animals & Nature", "ant", "bug"));
        list.add(new EmojiItem("🕷️", "spider", "🐶 Animals & Nature", "spider", "bug", "spooky"));
        list.add(new EmojiItem("🕸️", "spider web", "🐶 Animals & Nature", "web", "spooky"));
        list.add(new EmojiItem("🦂", "scorpion", "🐶 Animals & Nature", "scorpion", "bug"));
        list.add(new EmojiItem("🐢", "turtle", "🐶 Animals & Nature", "turtle", "reptile"));
        list.add(new EmojiItem("🐍", "snake", "🐶 Animals & Nature", "snake", "reptile"));
        list.add(new EmojiItem("🐙", "octopus", "🐶 Animals & Nature", "octopus", "sea", "animal"));
        list.add(new EmojiItem("🦑", "squid", "🐶 Animals & Nature", "squid", "sea"));
        list.add(new EmojiItem("🐠", "tropical fish", "🐶 Animals & Nature", "fish", "sea"));
        list.add(new EmojiItem("🐬", "dolphin", "🐶 Animals & Nature", "dolphin", "sea", "ocean"));
        list.add(new EmojiItem("🌳", "deciduous tree", "🐶 Animals & Nature", "tree", "forest", "nature"));
        list.add(new EmojiItem("🌴", "palm tree", "🐶 Animals & Nature", "tree", "palm", "beach"));
        list.add(new EmojiItem("🌱", "seedling", "🐶 Animals & Nature", "sprout", "nature", "grow"));
        list.add(new EmojiItem("🍀", "four leaf clover", "🐶 Animals & Nature", "clover", "lucky"));
        list.add(new EmojiItem("🍁", "maple leaf", "🐶 Animals & Nature", "leaf", "maple", "fall"));
        list.add(new EmojiItem("🔥", "fire", "🐶 Animals & Nature", "fire", "hot", "flame"));
        list.add(new EmojiItem("💧", "droplet", "🐶 Animals & Nature", "water", "drop", "rain"));

        // 4. 🍔 Food & Drink
        list.add(new EmojiItem("🍎", "red apple", "🍔 Food & Drink", "apple", "fruit"));
        list.add(new EmojiItem("🍌", "banana", "🍔 Food & Drink", "banana", "fruit"));
        list.add(new EmojiItem("🍉", "watermelon", "🍔 Food & Drink", "watermelon", "fruit"));
        list.add(new EmojiItem("🍇", "grapes", "🍔 Food & Drink", "grapes", "fruit"));
        list.add(new EmojiItem("🍓", "strawberry", "🍔 Food & Drink", "strawberry", "fruit"));
        list.add(new EmojiItem("🍒", "cherries", "🍔 Food & Drink", "cherry", "fruit"));
        list.add(new EmojiItem(" Pineapple", "pineapple", "🍔 Food & Drink", "pineapple", "fruit"));
        list.add(new EmojiItem("🥥", "coconut", "🍔 Food & Drink", "coconut", "fruit"));
        list.add(new EmojiItem("🥑", "avocado", "🍔 Food & Drink", "avocado", "fruit", "healthy"));
        list.add(new EmojiItem("🌽", "ear of corn", "🍔 Food & Drink", "corn", "vegetable"));
        list.add(new EmojiItem("🥕", "carrot", "🍔 Food & Drink", "carrot", "vegetable"));
        list.add(new EmojiItem("🧀", "cheese wedge", "🍔 Food & Drink", "cheese", "dairy"));
        list.add(new EmojiItem("🍳", "cooking egg", "🍔 Food & Drink", "egg", "fry", "breakfast"));
        list.add(new EmojiItem("🥞", "pancakes", "🍔 Food & Drink", "pancakes", "breakfast", "sweet"));
        list.add(new EmojiItem("🥓", "bacon", "🍔 Food & Drink", "bacon", "pork", "breakfast"));
        list.add(new EmojiItem("🥩", "cut of meat", "🍔 Food & Drink", "meat", "steak", "beef"));
        list.add(new EmojiItem("🍔", "hamburger", "🍔 Food & Drink", "burger", "fastfood"));
        list.add(new EmojiItem("🍟", "french fries", "🍔 Food & Drink", "fries", "potato", "fastfood"));
        list.add(new EmojiItem("🍕", "pizza", "🍔 Food & Drink", "pizza", "cheese", "fastfood"));
        list.add(new EmojiItem("🌭", "hot dog", "🍔 Food & Drink", "hotdog", "sausage", "fastfood"));
        list.add(new EmojiItem("🥪", "sandwich", "🍔 Food & Drink", "sandwich", "bread"));
        list.add(new EmojiItem("🌮", "taco", "🍔 Food & Drink", "taco", "mexican", "fastfood"));
        list.add(new EmojiItem("🌯", "burrito", "🍔 Food & Drink", "burrito", "mexican", "fastfood"));
        list.add(new EmojiItem("🍜", "steaming bowl", "🍔 Food & Drink", "ramen", "soup", "noodles"));
        list.add(new EmojiItem("🍣", "sushi", "🍔 Food & Drink", "sushi", "fish"));
        list.add(new EmojiItem("🍦", "soft ice cream", "🍔 Food & Drink", "icecream", "sweet"));
        list.add(new EmojiItem("🍩", "doughnut", "🍔 Food & Drink", "donut", "sweet"));
        list.add(new EmojiItem("🍪", "cookie", "🍔 Food & Drink", "cookie", "sweet"));
        list.add(new EmojiItem("🎂", "birthday cake", "🍔 Food & Drink", "cake", "birthday", "sweet"));
        list.add(new EmojiItem("🍰", "shortcake", "🍔 Food & Drink", "cake", "sweet"));
        list.add(new EmojiItem("🍫", "chocolate bar", "🍔 Food & Drink", "chocolate", "sweet"));
        list.add(new EmojiItem("🍬", "candy", "🍔 Food & Drink", "candy", "sweet"));
        list.add(new EmojiItem("🥤", "cup with straw", "🍔 Food & Drink", "soda", "drink"));
        list.add(new EmojiItem("🧋", "bubble tea", "🍔 Food & Drink", "bubbletea", "boba", "tea"));
        list.add(new EmojiItem("☕", "hot beverage", "🍔 Food & Drink", "coffee", "tea", "hot"));
        list.add(new EmojiItem("🍷", "wine glass", "🍔 Food & Drink", "wine", "alcohol"));
        list.add(new EmojiItem("🍺", "beer mug", "🍔 Food & Drink", "beer", "alcohol"));
        list.add(new EmojiItem("🍻", "clinking beer mugs", "🍔 Food & Drink", "beers", "cheers", "alcohol"));

        // 5. 🌍 Travel & Places
        list.add(new EmojiItem("🌍", "globe Europe-Africa", "🌍 Travel & Places", "globe", "world", "earth"));
        list.add(new EmojiItem("🚗", "automobile", "🌍 Travel & Places", "car", "drive", "travel"));
        list.add(new EmojiItem("🚕", "taxi", "🌍 Travel & Places", "taxi", "cab"));
        list.add(new EmojiItem("🏎️", "racing car", "🌍 Travel & Places", "race", "car", "speed"));
        list.add(new EmojiItem("🏍️", "motorcycle", "🌍 Travel & Places", "motorcycle", "bike"));
        list.add(new EmojiItem("🚲", "bicycle", "🌍 Travel & Places", "bike", "bicycle"));
        list.add(new EmojiItem("🚌", "bus", "🌍 Travel & Places", "bus", "transport"));
        list.add(new EmojiItem("✈️", "airplane", "🌍 Travel & Places", "plane", "travel", "flight"));
        list.add(new EmojiItem("🚀", "rocket", "🌍 Travel & Places", "rocket", "space", "launch"));
        list.add(new EmojiItem("🚂", "locomotive", "🌍 Travel & Places", "train", "travel"));
        list.add(new EmojiItem("⛵", "sailboat", "🌍 Travel & Places", "boat", "sail", "sea"));
        list.add(new EmojiItem("🗺️", "world map", "🌍 Travel & Places", "map", "world", "explore"));
        list.add(new EmojiItem("🏠", "house", "🌍 Travel & Places", "house", "home"));
        list.add(new EmojiItem("🏢", "office building", "🌍 Travel & Places", "office", "work", "building"));
        list.add(new EmojiItem("🏥", "hospital", "🌍 Travel & Places", "hospital", "doctor", "health"));
        list.add(new EmojiItem("🏫", "school", "🌍 Travel & Places", "school", "education", "class"));
        list.add(new EmojiItem("⛪", "church", "🌍 Travel & Places", "church", "religion"));
        list.add(new EmojiItem("🕌", "mosque", "🌍 Travel & Places", "mosque", "religion", "islam"));
        list.add(new EmojiItem("🌋", "volcano", "🌍 Travel & Places", "volcano", "lava", "nature"));
        list.add(new EmojiItem("🏖️", "beach with umbrella", "🌍 Travel & Places", "beach", "summer", "sun", "vacation"));
        list.add(new EmojiItem("☀️", "sun", "🌍 Travel & Places", "sun", "sunny", "hot", "weather"));
        list.add(new EmojiItem("⭐", "star", "🌍 Travel & Places", "star", "yellow"));
        list.add(new EmojiItem("☁️", "cloud", "🌍 Travel & Places", "cloud", "cloudy", "weather"));
        list.add(new EmojiItem("🌧️", "cloud with rain", "🌍 Travel & Places", "rain", "weather"));
        list.add(new EmojiItem("❄️", "snowflake", "🌍 Travel & Places", "snow", "cold", "winter"));
        list.add(new EmojiItem("🌈", "rainbow", "🌍 Travel & Places", "rainbow", "color", "nature"));

        // 6. ⚽ Activities
        list.add(new EmojiItem("⚽", "soccer ball", "⚽ Activities", "soccer", "football", "sport"));
        list.add(new EmojiItem("🏀", "basketball", "⚽ Activities", "basketball", "ball", "sport"));
        list.add(new EmojiItem("🏈", "american football", "⚽ Activities", "football", "sport"));
        list.add(new EmojiItem("⚾", "baseball", "⚽ Activities", "baseball", "sport"));
        list.add(new EmojiItem("🎾", "tennis", "⚽ Activities", "tennis", "sport"));
        list.add(new EmojiItem("🏐", "volleyball", "⚽ Activities", "volleyball", "sport"));
        list.add(new EmojiItem("🎱", "pool 8 ball", "⚽ Activities", "pool", "billiards", "game"));
        list.add(new EmojiItem("⛳", "flag in hole", "⚽ Activities", "golf", "sport"));
        list.add(new EmojiItem("🎯", "bullseye", "⚽ Activities", "darts", "target", "game"));
        list.add(new EmojiItem("🎮", "video game", "⚽ Activities", "game", "controller", "gaming"));
        list.add(new EmojiItem("🎲", "game die", "⚽ Activities", "dice", "die", "game"));
        list.add(new EmojiItem("🎨", "artist palette", "⚽ Activities", "art", "paint", "draw"));
        list.add(new EmojiItem("🎬", "clapper board", "⚽ Activities", "movie", "film", "cinema"));
        list.add(new EmojiItem("🎤", "microphone", "⚽ Activities", "sing", "music", "mic", "karaoke"));
        list.add(new EmojiItem("🎧", "headphones", "⚽ Activities", "music", "audio", "listen"));
        list.add(new EmojiItem("🎸", "guitar", "⚽ Activities", "guitar", "music", "instrument"));
        list.add(new EmojiItem("🎹", "musical keyboard", "⚽ Activities", "piano", "music", "instrument"));
        list.add(new EmojiItem("🏆", "trophy", "⚽ Activities", "trophy", "winner", "award"));

        // 7. 📦 Objects
        list.add(new EmojiItem("💡", "light bulb", "📦 Objects", "light", "bulb", "idea"));
        list.add(new EmojiItem("🔦", "flashlight", "📦 Objects", "light", "flashlight", "torch"));
        list.add(new EmojiItem("🗑️", "wastebasket", "📦 Objects", "trash", "bin", "waste"));
        list.add(new EmojiItem("💸", "money with wings", "📦 Objects", "money", "cash", "spend"));
        list.add(new EmojiItem("💵", "dollar bill", "📦 Objects", "money", "cash", "dollar"));
        list.add(new EmojiItem("🪙", "coin", "📦 Objects", "money", "coin", "gold"));
        list.add(new EmojiItem("💰", "money bag", "📦 Objects", "money", "bag", "rich"));
        list.add(new EmojiItem("💳", "credit card", "📦 Objects", "card", "pay", "money"));
        list.add(new EmojiItem("💎", "gem stone", "📦 Objects", "gem", "diamond", "jewelry"));
        list.add(new EmojiItem("⚖️", "balance scale", "📦 Objects", "scale", "law", "justice"));
        list.add(new EmojiItem("🧰", "toolbox", "📦 Objects", "tool", "toolbox", "repair"));
        list.add(new EmojiItem("🔧", "wrench", "📦 Objects", "tool", "wrench", "repair"));
        list.add(new EmojiItem("🔨", "hammer", "📦 Objects", "tool", "hammer", "build"));
        list.add(new EmojiItem("⚔️", "crossed swords", "📦 Objects", "swords", "fight"));
        list.add(new EmojiItem("🛡️", "shield", "📦 Objects", "shield", "protect", "security"));
        list.add(new EmojiItem("🔑", "key", "📦 Objects", "key", "lock", "unlock"));
        list.add(new EmojiItem("🔒", "locked", "📦 Objects", "lock", "closed", "secure"));
        list.add(new EmojiItem("🔓", "unlocked", "📦 Objects", "lock", "open"));
        list.add(new EmojiItem("✉️", "envelope", "📦 Objects", "mail", "letter", "post"));
        list.add(new EmojiItem("📦", "package", "📦 Objects", "box", "package", "delivery"));
        list.add(new EmojiItem("📱", "mobile phone", "📦 Objects", "phone", "mobile", "smartphone"));
        list.add(new EmojiItem("💻", "laptop", "📦 Objects", "computer", "laptop", "pc"));
        list.add(new EmojiItem("🖥️", "desktop computer", "📦 Objects", "computer", "pc", "desktop"));
        list.add(new EmojiItem("📷", "camera", "📦 Objects", "camera", "photo", "picture"));
        list.add(new EmojiItem("📚", "books", "📦 Objects", "books", "read", "study"));
        list.add(new EmojiItem("📝", "memo", "📦 Objects", "note", "paper", "write"));
        list.add(new EmojiItem("🎁", "wrapped gift", "📦 Objects", "gift", "present", "birthday"));

        // 8. 🔣 Symbols
        list.add(new EmojiItem("💘", "heart with arrow", "🔣 Symbols", "love", "heart"));
        list.add(new EmojiItem("💝", "heart with ribbon", "🔣 Symbols", "love", "heart", "gift"));
        list.add(new EmojiItem("💖", "sparkling heart", "🔣 Symbols", "love", "heart", "sparkles"));
        list.add(new EmojiItem("💗", "growing heart", "🔣 Symbols", "love", "heart"));
        list.add(new EmojiItem("💓", "beating heart", "🔣 Symbols", "love", "heart"));
        list.add(new EmojiItem("💞", "revolving hearts", "🔣 Symbols", "love", "hearts"));
        list.add(new EmojiItem("💕", "two hearts", "🔣 Symbols", "love", "hearts"));
        list.add(new EmojiItem("💟", "heart decoration", "🔣 Symbols", "love", "heart"));
        list.add(new EmojiItem("❣️", "heart exclamation", "🔣 Symbols", "love", "heart", "exclamation"));
        list.add(new EmojiItem("💔", "broken heart", "🔣 Symbols", "love", "heart", "break", "sad"));
        list.add(new EmojiItem("☮️", "peace symbol", "🔣 Symbols", "peace"));
        list.add(new EmojiItem("✝️", "latin cross", "🔣 Symbols", "cross", "religion"));
        list.add(new EmojiItem("☪️", "star and crescent", "🔣 Symbols", "crescent", "religion", "islam"));
        list.add(new EmojiItem("🕉️", "om", "🔣 Symbols", "om", "religion"));
        list.add(new EmojiItem("✡️", "star of david", "🔣 Symbols", "star", "religion", "jewish"));
        list.add(new EmojiItem("☯️", "yin yang", "🔣 Symbols", "yin", "yang", "balance"));
        list.add(new EmojiItem("➕", "plus sign", "🔣 Symbols", "plus", "math"));
        list.add(new EmojiItem("➖", "minus sign", "🔣 Symbols", "minus", "math"));
        list.add(new EmojiItem("✖️", "multiply sign", "🔣 Symbols", "multiply", "math"));
        list.add(new EmojiItem("➗", "divide sign", "🔣 Symbols", "divide", "math"));
        list.add(new EmojiItem("❓", "question mark", "🔣 Symbols", "question", "ask"));
        list.add(new EmojiItem("❗", "exclamation mark", "🔣 Symbols", "exclamation", "alert", "warning"));
        list.add(new EmojiItem("💯", "hundred points", "🔣 Symbols", "100", "perfect"));
        list.add(new EmojiItem("🚫", "prohibited", "🔣 Symbols", "stop", "no", "ban"));

        // 9. 🏳️ Flags
        list.add(new EmojiItem("🏳️", "white flag", "🏳️ Flags", "flag", "white", "peace"));
        list.add(new EmojiItem("🏴", "black flag", "🏳️ Flags", "flag", "black"));
        list.add(new EmojiItem("🏁", "chequered flag", "🏳️ Flags", "flag", "race", "checkered"));
        list.add(new EmojiItem("🚩", "triangular flag", "🏳️ Flags", "flag", "red", "warn"));
        list.add(new EmojiItem("🏳️‍🌈", "rainbow flag", "🏳️ Flags", "flag", "rainbow", "pride"));
        list.add(new EmojiItem("🏳️‍⚧️", "transgender flag", "🏳️ Flags", "flag", "pride", "trans"));
        list.add(new EmojiItem("🏴‍☠️", "pirate flag", "🏳️ Flags", "flag", "pirate", "skull"));
        list.add(new EmojiItem("🇧🇩", "Bangladesh flag", "🏳️ Flags", "flag", "bangladesh", "bd"));
        list.add(new EmojiItem("🇺🇸", "US flag", "🏳️ Flags", "flag", "us", "america", "usa"));
        list.add(new EmojiItem("🇬🇧", "UK flag", "🏳️ Flags", "flag", "uk", "britain"));
        list.add(new EmojiItem("🇨🇦", "Canada flag", "🏳️ Flags", "flag", "canada", "ca"));
        list.add(new EmojiItem("🇯🇵", "Japan flag", "🏳️ Flags", "flag", "japan", "jp"));
        list.add(new EmojiItem("🇮🇳", "India flag", "🏳️ Flags", "flag", "india", "in"));
        list.add(new EmojiItem("🇩🇪", "Germany flag", "🏳️ Flags", "flag", "germany", "de"));
        list.add(new EmojiItem("🇫🇷", "France flag", "🏳️ Flags", "flag", "france", "fr"));
        list.add(new EmojiItem("🇧🇷", "Brazil flag", "🏳️ Flags", "flag", "brazil", "br"));
        list.add(new EmojiItem("🇦🇺", "Australia flag", "🏳️ Flags", "flag", "australia", "au"));

        // 10. 👨‍👩‍👧‍👦 Component
        list.add(new EmojiItem("🏻", "light skin tone", "👨‍👩‍👧‍👦 Component", "skin", "tone", "light", "type1", "type2"));
        list.add(new EmojiItem("🏼", "medium-light skin tone", "👨‍👩‍👧‍👦 Component", "skin", "tone", "medium", "type3"));
        list.add(new EmojiItem("🏽", "medium skin tone", "👨‍👩‍👧‍👦 Component", "skin", "tone", "medium", "type4"));
        list.add(new EmojiItem("🏾", "medium-dark skin tone", "👨‍👩‍👧‍👦 Component", "skin", "tone", "dark", "type5"));
        list.add(new EmojiItem("🏿", "dark skin tone", "👨‍👩‍👧‍👦 Component", "skin", "tone", "dark", "type6"));
        list.add(new EmojiItem("🦰", "red hair", "👨‍👩‍👧‍👦 Component", "hair", "red"));
        list.add(new EmojiItem("🦱", "curly hair", "👨‍👩‍👧‍👦 Component", "hair", "curly"));
        list.add(new EmojiItem("白", "white hair", "👨‍👩‍👧‍👦 Component", "hair", "white"));
        list.add(new EmojiItem("🦲", "bald", "👨‍👩‍👧‍👦 Component", "hair", "bald", "shaved"));
        list.add(new EmojiItem("👍🏻", "thumbs up light", "👨‍👩‍👧‍👦 Component", "thumbsup", "like", "light"));
        list.add(new EmojiItem("👍🏼", "thumbs up medium-light", "👨‍👩‍👧‍👦 Component", "thumbsup", "like", "medium"));
        list.add(new EmojiItem("👍🏽", "thumbs up medium", "👨‍👩‍👧‍👦 Component", "thumbsup", "like", "medium"));
        list.add(new EmojiItem("👍🏾", "thumbs up medium-dark", "👨‍👩‍👧‍👦 Component", "thumbsup", "like", "dark"));
        list.add(new EmojiItem("👍🏿", "thumbs up dark", "👨‍👩‍👧‍👦 Component", "thumbsup", "like", "dark"));
        list.add(new EmojiItem("👎🏻", "thumbs down light", "👨‍👩‍👧‍👦 Component", "thumbsdown", "dislike", "light"));
        list.add(new EmojiItem("👎🏼", "thumbs down medium-light", "👨‍👩‍👧‍👦 Component", "thumbsdown", "dislike", "medium"));
        list.add(new EmojiItem("👎🏽", "thumbs down medium", "👨‍👩‍👧‍👦 Component", "thumbsdown", "dislike", "medium"));
        list.add(new EmojiItem("👎🏾", "thumbs down medium-dark", "👨‍👩‍👧‍👦 Component", "thumbsdown", "dislike", "dark"));
        list.add(new EmojiItem("👎🏿", "thumbs down dark", "👨‍👩‍👧‍👦 Component", "thumbsdown", "dislike", "dark"));
        list.add(new EmojiItem("👋🏻", "waving hand light", "👨‍👩‍👧‍👦 Component", "wave", "light"));
        list.add(new EmojiItem("👋🏼", "waving hand medium-light", "👨‍👩‍👧‍👦 Component", "wave", "medium"));
        list.add(new EmojiItem("👋🏽", "waving hand medium", "👨‍👩‍👧‍👦 Component", "wave", "medium"));
        list.add(new EmojiItem("👋🏾", "waving hand medium-dark", "👨‍👩‍👧‍👦 Component", "wave", "dark"));
        list.add(new EmojiItem("👋🏿", "waving hand dark", "👨‍👩‍👧‍👦 Component", "wave", "dark"));
        list.add(new EmojiItem("👨‍👩‍👧‍👦", "family man woman girl boy", "👨‍👩‍👧‍👦 Component", "family", "man", "woman", "children", "zwj"));
        list.add(new EmojiItem("👨‍👩‍👦", "family man woman boy", "👨‍👩‍👧‍👦 Component", "family", "man", "woman", "boy", "zwj"));
        list.add(new EmojiItem("👨‍👩‍👧", "family man woman girl", "👨‍👩‍👧‍👦 Component", "family", "man", "woman", "girl", "zwj"));
        list.add(new EmojiItem("🧑‍⚕️", "health worker", "👨‍👩‍👧‍👦 Component", "doctor", "nurse", "health", "zwj"));
        list.add(new EmojiItem("🧑‍🎓", "student", "👨‍👩‍👧‍👦 Component", "graduate", "student", "college", "zwj"));
        list.add(new EmojiItem("🧑‍🏫", "teacher", "👨‍👩‍👧‍👦 Component", "teacher", "professor", "class", "zwj"));
        list.add(new EmojiItem("🧑‍🍳", "cook", "👨‍👩‍👧‍👦 Component", "chef", "cook", "kitchen", "zwj"));
        list.add(new EmojiItem("🧑‍💻", "technologist", "👨‍👩‍👧‍👦 Component", "coder", "programmer", "developer", "computer", "zwj"));

        allEmojis = Collections.unmodifiableList(list);
        return allEmojis;
    }
}
