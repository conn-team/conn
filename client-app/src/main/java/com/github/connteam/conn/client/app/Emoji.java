package com.github.connteam.conn.client.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class Emoji {
    private static final List<Emoji> emojis = new ArrayList<>();

    private final String primaryCode;
    private final Pattern pattern;
    private final Image image;

    public Emoji(String primaryCode, Pattern pattern, Image image) {
        this.primaryCode = primaryCode;
        this.pattern = pattern;
        this.image = image;
    }

    public String getPrimaryCode() {
        return primaryCode;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Image getImage() {
        return image;
    }

    public static List<Emoji> getEmojis() {
        return emojis;
    }

    private static void registerEmoji(String primaryCode, String patternStr, String resource) {
        Pattern pattern = Pattern.compile("(^|(?<=\\s))(?i:" + patternStr + ")($|(?=\\s))");
        Image img = new Image(Emoji.class.getClassLoader().getResourceAsStream(resource));
        emojis.add(new Emoji(primaryCode, pattern, img));
    }

    public static void setTextFlowEmojiText(TextFlow flow, String raw) {
        flow.getChildren().clear();
        replaceEmojisRecursive(flow, raw, 0);
    }

    private static void replaceEmojisRecursive(TextFlow flow, String raw, int emojiIndex) {
        if (emojiIndex >= emojis.size()) {
            if (raw.length() > 0) {
                flow.getChildren().add(new Text(raw));
            }
            return;
        }

        Emoji emoji = emojis.get(emojiIndex);
        String[] parts = emoji.getPattern().split(raw, -1);

        replaceEmojisRecursive(flow, parts[0], emojiIndex + 1);

        for (int i = 1; i < parts.length; i++) {
            ImageView img = new ImageView(emoji.getImage()) {
                @Override
                public double getBaselineOffset() {
                    return 16;
                }
            };

            img.setFitHeight(22);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            img.setCache(true);
            flow.getChildren().add(img);

            replaceEmojisRecursive(flow, parts[i], emojiIndex + 1);
        }
    }

    static {
        registerEmoji(":3", ";3|:3|:malcin:", "emojis/malcin.png");

        registerEmoji(":smile:", ":D|:smile:", "emojis/twemoji/1F604.png");
        registerEmoji(":smiley:", ":smiley:", "emojis/twemoji/1F603.png");
        registerEmoji(":grinning:", ":\\)|:grinning:", "emojis/twemoji/1F600.png");
        registerEmoji(":blush:", ":blush:", "emojis/twemoji/1F60A.png");
        registerEmoji(":relaxed:", ":relaxed:", "emojis/twemoji/263A.png");
        registerEmoji(":wink:", ";\\)|:wink:", "emojis/twemoji/1F609.png");
        registerEmoji(":heart_eyes:", ":heart_eyes:", "emojis/twemoji/1F60D.png");
        registerEmoji(":kissing_heart:", ";\\*|:\\*|:kissing_heart:", "emojis/twemoji/1F618.png");
        registerEmoji(":kissing_closed_eyes:", ":kissing_closed_eyes:", "emojis/twemoji/1F61A.png");
        registerEmoji(":kissing:", ":kissing:", "emojis/twemoji/1F617.png");
        registerEmoji(":kissing_smiling_eyes:", ":kissing_smiling_eyes:", "emojis/twemoji/1F619.png");
        registerEmoji(":stuck_out_tongue_winking_eye:", ":stuck_out_tongue_winking_eye:", "emojis/twemoji/1F61C.png");
        registerEmoji(":stuck_out_tongue_closed_eyes:", ":stuck_out_tongue_closed_eyes:", "emojis/twemoji/1F61D.png");
        registerEmoji(":stuck_out_tongue:", ":stuck_out_tongue:", "emojis/twemoji/1F61B.png");
        registerEmoji(":flushed:", ":flushed:", "emojis/twemoji/1F633.png");
        registerEmoji(":grin:", ":grin:", "emojis/twemoji/1F601.png");
        registerEmoji(":pensive:", ":pensive:", "emojis/twemoji/1F614.png");
        registerEmoji(":relieved:", ":relieved:", "emojis/twemoji/1F60C.png");
        registerEmoji(":unamused:", ":unamused:", "emojis/twemoji/1F612.png");
        registerEmoji(":disappointed:", ":\\(|:disappointed:", "emojis/twemoji/1F61E.png");
        registerEmoji(":persevere:", ":persevere:", "emojis/twemoji/1F623.png");
        registerEmoji(":cry:", ";\\(|:cry:", "emojis/twemoji/1F622.png");
        registerEmoji(":joy:", ":joy:", "emojis/twemoji/1F602.png");
        registerEmoji(":sob:", ":sob:", "emojis/twemoji/1F62D.png");
        registerEmoji(":sleepy:", ":sleepy:", "emojis/twemoji/1F62A.png");
        registerEmoji(":disappointed_relieved:", ":disappointed_relieved:", "emojis/twemoji/1F625.png");
        registerEmoji(":cold_sweat:", ":cold_sweat:", "emojis/twemoji/1F630.png");
        registerEmoji(":sweat_smile:", ":sweat_smile:", "emojis/twemoji/1F605.png");
        registerEmoji(":sweat:", ":sweat:", "emojis/twemoji/1F613.png");
        registerEmoji(":weary:", ":weary:", "emojis/twemoji/1F629.png");
        registerEmoji(":tired_face:", ":tired_face:", "emojis/twemoji/1F62B.png");
        registerEmoji(":fearful:", ":fearful:", "emojis/twemoji/1F628.png");
        registerEmoji(":scream:", ":scream:", "emojis/twemoji/1F631.png");
        registerEmoji(":angry:", ">:\\(|:angry:", "emojis/twemoji/1F620.png");
        registerEmoji(":rage:", ":rage:", "emojis/twemoji/1F621.png");
        registerEmoji(":triumph:", ":triumph:", "emojis/twemoji/1F624.png");
        registerEmoji(":confounded:", ":confounded:", "emojis/twemoji/1F616.png");
        registerEmoji(":laughing:", ":laughing:|:satisfied:", "emojis/twemoji/1F606.png");
        registerEmoji(":yum:", ":yum:", "emojis/twemoji/1F60B.png");
        registerEmoji(":mask:", ":mask:", "emojis/twemoji/1F637.png");
        registerEmoji(":sunglasses:", ":sunglasses:", "emojis/twemoji/1F60E.png");
        registerEmoji(":sleeping:", ":sleeping:", "emojis/twemoji/1F634.png");
        registerEmoji(":dizzy_face:", ":dizzy_face:", "emojis/twemoji/1F635.png");
        registerEmoji(":astonished:", ":astonished:", "emojis/twemoji/1F632.png");
        registerEmoji(":worried:", ":worried:", "emojis/twemoji/1F61F.png");
        registerEmoji(":frowning:", ":frowning:", "emojis/twemoji/1F626.png");
        registerEmoji(":anguished:", ":anguished:", "emojis/twemoji/1F627.png");
        registerEmoji(":smiling_imp:", ":smiling_imp:", "emojis/twemoji/1F608.png");
        registerEmoji(":imp:", ":imp:", "emojis/twemoji/1F47F.png");
        registerEmoji(":open_mouth:", ":open_mouth:", "emojis/twemoji/1F62E.png");
        registerEmoji(":grimacing:", ":grimacing:", "emojis/twemoji/1F62C.png");
        registerEmoji(":neutral_face:", ":neutral_face:", "emojis/twemoji/1F610.png");
        registerEmoji(":confused:", ":confused:", "emojis/twemoji/1F615.png");
        registerEmoji(":hushed:", ":O|:hushed:", "emojis/twemoji/1F62F.png");
        registerEmoji(":no_mouth:", ":no_mouth:", "emojis/twemoji/1F636.png");
        registerEmoji(":innocent:", ":innocent:", "emojis/twemoji/1F607.png");
        registerEmoji(":smirk:", ":smirk:", "emojis/twemoji/1F60F.png");
        registerEmoji(":expressionless:", ":expressionless:", "emojis/twemoji/1F611.png");
        registerEmoji(":thinking:", ":thinking:|:thinking_face:", "emojis/twemoji/1F914.png");
    }
}
