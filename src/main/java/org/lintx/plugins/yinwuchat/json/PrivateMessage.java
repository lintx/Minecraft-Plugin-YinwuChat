package org.lintx.plugins.yinwuchat.json;

import java.util.ArrayList;
import java.util.List;

public class PrivateMessage extends Message {
    //me -> toplayer > message
    public List<MessageFormat> toFormat = new ArrayList<>();
    //toplayer -> me > message
    public List<MessageFormat> fromFormat = new ArrayList<>();
    public String toPlayer = "";
}
