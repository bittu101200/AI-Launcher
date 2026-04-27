package bhupendra.ai.launcher.managers.xml.options;

import bhupendra.ai.launcher.managers.notifications.reply.ReplyManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsElement;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 17/01/2018.
 */

public enum Reply implements XMLPrefsSave {

    reply_enabled {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }

        @Override
        public String info() {
            return "If false, notification reply will be disabled";
        }
    };

    @Override
    public XMLPrefsElement parent() {
        return ReplyManager.instance;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public String[] invalidValues() {
        return null;
    }

    @Override
    public String getLowercaseString() {
        return label();
    }

    @Override
    public String getString() {
        return label();
    }
}
