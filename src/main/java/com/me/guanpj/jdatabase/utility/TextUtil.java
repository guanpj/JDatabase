package com.me.guanpj.jdatabase.utility;

import java.util.ArrayList;

/**
 * Created by Jie on 2017/4/18.
 */

public class TextUtil {
    public static boolean isValidate(String content){
        if(content != null && !"".equals(content.trim())){
            return true;
        }
        return false;
    }

    public static boolean isValidate(ArrayList list){
        if (list != null && list.size() >0) {
            return true;
        }
        return false;
    }
}
