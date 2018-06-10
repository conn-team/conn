package com.github.connteam.conn.client.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class Emoji {
    private static List<Pair<Pattern, Image>> emojis = new ArrayList<>();

    private static void registerEmoji(String patternStr, String resource) {
        Pattern pattern = Pattern.compile("(?i:" + patternStr + ")");
        Image img = new Image(Emoji.class.getClassLoader().getResourceAsStream(resource));
        emojis.add(Pair.of(pattern, img));
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

        Pair<Pattern, Image> emoji = emojis.get(emojiIndex);
        String[] parts = emoji.getKey().split(raw, -1);

        replaceEmojisRecursive(flow, parts[0], emojiIndex + 1);

        for (int i = 1; i < parts.length; i++) {
            ImageView img = new ImageView(emoji.getValue()) {
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
        registerEmoji(";3|:3", "emojis/malcin.png");

        registerEmoji(":D|:smile:", "emojis/emojione/1F604.png");
        registerEmoji(":smiley:", "emojis/emojione/1F603.png");
        registerEmoji(":grinning:", "emojis/emojione/1F600.png");
        registerEmoji(":blush:", "emojis/emojione/1F60A.png");
        registerEmoji(":relaxed:", "emojis/emojione/263A.png");
        registerEmoji(";)|:wink:", "emojis/emojione/1F609.png");
        registerEmoji(":heart_eyes:", "emojis/emojione/1F60D.png");
        registerEmoji(";*|:*|:kissing_heart:", "emojis/emojione/1F618.png");
        registerEmoji(":kissing_closed_eyes:", "emojis/emojione/1F61A.png");
        registerEmoji(":kissing:", "emojis/emojione/1F617.png");
        registerEmoji(":kissing_smiling_eyes:", "emojis/emojione/1F619.png");
        registerEmoji(":stuck_out_tongue_winking_eye:", "emojis/emojione/1F61C.png");
        registerEmoji(":stuck_out_tongue_closed_eyes:", "emojis/emojione/1F61D.png");
        registerEmoji(":stuck_out_tongue:", "emojis/emojione/1F61B.png");
        registerEmoji(":flushed:", "emojis/emojione/1F633.png");
        registerEmoji(":grin:", "emojis/emojione/1F601.png");
        registerEmoji(":pensive:", "emojis/emojione/1F614.png");
        registerEmoji(":relieved:", "emojis/emojione/1F60C.png");
        registerEmoji(":unamused:", "emojis/emojione/1F612.png");
        registerEmoji(":disappointed:", "emojis/emojione/1F61E.png");
        registerEmoji(":persevere:", "emojis/emojione/1F623.png");
        registerEmoji(";(|:cry:", "emojis/emojione/1F622.png");
        registerEmoji(":joy:", "emojis/emojione/1F602.png");
        registerEmoji(":sob:", "emojis/emojione/1F62D.png");
        registerEmoji(":sleepy:", "emojis/emojione/1F62A.png");
        registerEmoji(":disappointed_relieved:", "emojis/emojione/1F625.png");
        registerEmoji(":cold_sweat:", "emojis/emojione/1F630.png");
        registerEmoji(":sweat_smile:", "emojis/emojione/1F605.png");
        registerEmoji(":sweat:", "emojis/emojione/1F613.png");
        registerEmoji(":weary:", "emojis/emojione/1F629.png");
        registerEmoji(":tired_face:", "emojis/emojione/1F62B.png");
        registerEmoji(":fearful:", "emojis/emojione/1F628.png");
        registerEmoji(":scream:", "emojis/emojione/1F631.png");
        registerEmoji(">:(|:angry:", "emojis/emojione/1F620.png");
        registerEmoji(":rage:", "emojis/emojione/1F621.png");
        registerEmoji(":triumph:", "emojis/emojione/1F624.png");
        registerEmoji(":confounded:", "emojis/emojione/1F616.png");
        registerEmoji(":laughing:|:satisfied:", "emojis/emojione/1F606.png");
        registerEmoji(":yum:", "emojis/emojione/1F60B.png");
        registerEmoji(":mask:", "emojis/emojione/1F637.png");
        registerEmoji(":sunglasses:", "emojis/emojione/1F60E.png");
        registerEmoji(":sleeping:", "emojis/emojione/1F634.png");
        registerEmoji(":dizzy_face:", "emojis/emojione/1F635.png");
        registerEmoji(":astonished:", "emojis/emojione/1F632.png");
        registerEmoji(":worried:", "emojis/emojione/1F61F.png");
        registerEmoji(":frowning:", "emojis/emojione/1F626.png");
        registerEmoji(":anguished:", "emojis/emojione/1F627.png");
        registerEmoji(":smiling_imp:", "emojis/emojione/1F608.png");
        registerEmoji(":imp:", "emojis/emojione/1F47F.png");
        registerEmoji(":open_mouth:", "emojis/emojione/1F62E.png");
        registerEmoji(":grimacing:", "emojis/emojione/1F62C.png");
        registerEmoji(":neutral_face:", "emojis/emojione/1F610.png");
        registerEmoji(":confused:", "emojis/emojione/1F615.png");
        registerEmoji(":hushed:", "emojis/emojione/1F62F.png");
        registerEmoji(":no_mouth:", "emojis/emojione/1F636.png");
        registerEmoji(":innocent:", "emojis/emojione/1F607.png");
        registerEmoji(":smirk:", "emojis/emojione/1F60F.png");
        registerEmoji(":expressionless:", "emojis/emojione/1F611.png");
    }
}
