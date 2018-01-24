package com.bms.util.commmon;

/**
 * 作者：黄国强
 * 时间：2017/2/24
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;

public final class SqlUtil {
    private static final String DEFAULT_SPLIT_STR = ",\u001c";
    private static final Log logger = LogFactory.getLog(SqlUtil.class);

    public SqlUtil() {
    }

    public static String cleanSql(String sql) {
        String result = null;
        if(sql != null) {
            String commentRemovedSql = removeComment(sql);
            result = removeBlank(commentRemovedSql);
        }

        return result;
    }

    private static String removeComment(String sql) {
        assert sql != null : "The parameter sql SHOULD NOT be null!";

        String[] sqlFragmentArr = StringUtil.split(sql, "/*");
        StringBuilder builder1 = new StringBuilder();
        String[] reader = sqlFragmentArr;
        int builder2 = sqlFragmentArr.length;

        String sqlFragment;
        int lineCommentStrIdx;
        for(int line = 0; line < builder2; ++line) {
            sqlFragment = reader[line];
            lineCommentStrIdx = sqlFragment.indexOf("*/");
            if(lineCommentStrIdx != -1) {
                sqlFragment = sqlFragment.substring(lineCommentStrIdx + 2);
            }

            builder1.append(sqlFragment);
        }

        BufferedReader var9 = new BufferedReader(new StringReader(builder1.toString()));
        StringBuilder var10 = new StringBuilder();
        String var11 = null;

        while(true) {
            try {
                var11 = var9.readLine();
            } catch (IOException var8) {
                logger.error("SqlUtil.removeComment,去掉注释失败：" + var8.getMessage());
            }

            if(null == var11) {
                return var10.toString();
            }

            sqlFragment = var11;
            lineCommentStrIdx = var11.indexOf("--");
            if(lineCommentStrIdx != -1) {
                sqlFragment = var11.substring(0, lineCommentStrIdx);
            }

            var10.append(sqlFragment);
            var10.append("\r\n");
        }
    }

    private static String removeBlank(String sql) {
        assert sql != null : "The parameter sql SHOULD NOT be null!";

        StringBuilder builder = new StringBuilder();
        String trimedSql = sql.trim();
        if(!trimedSql.isEmpty()) {
            char[] charArr = trimedSql.toCharArray();
            boolean hasBlank = false;
            boolean isInString = true;

            for(int i = 0; i < charArr.length; ++i) {
                char c = charArr[i];
                if(c == 39) {
                    builder.append(c);
                    isInString = !isInString;
                } else if(c <= 32) {
                    if(!isInString || !hasBlank) {
                        builder.append(' ');
                        hasBlank = true;
                    }
                } else {
                    builder.append(c);
                    hasBlank = false;
                }
            }
        }

        return builder.toString();
    }

    public static String[] split(String param) {
        return StringUtil.split(param, ",\u001c");
    }

    /** @deprecated */
    @Deprecated
    public static String format4in(String sqlFragment) {
        return format4in(sqlFragment, ",\u001c", false);
    }

    public static String format4in(String sqlFragment, char splitChar) {
        return format4in(sqlFragment, splitChar, false);
    }

    public static String format4in(String sqlFragment, char splitChar, boolean isNumber) {
        String[] fragmentArr = StringUtil.split(sqlFragment, splitChar);
        return rebuild(fragmentArr, String.valueOf(splitChar), isNumber);
    }

    public static String format4in(String sqlFragment, String splitStr) {
        return format4in(sqlFragment, splitStr, false);
    }

    public static String format4in(String sqlFragment, String splitStr, boolean isNumber) {
        String[] fragmentArr = StringUtil.split(sqlFragment, splitStr);
        return rebuild(fragmentArr, splitStr, isNumber);
    }

    private static String rebuild(String[] fragmentArr, String splitStr, boolean isNumber) {
        assert fragmentArr != null : "The parameter fragmentArr SHOULD NOT be null!";

        assert splitStr != null : "The parameter splitStr SHOULD NOT be null!";

        assert !splitStr.isEmpty() : "The parameter splitStr SHOULD NOT be empty!";

        LinkedList fragmentList = new LinkedList();
        StringBuilder buff = new StringBuilder();
        String[] fragmentBuff = fragmentArr;
        int result = fragmentArr.length;

        for(int fragment = 0; fragment < result; ++fragment) {
            String fragment1 = fragmentBuff[fragment];
            buff.append(fragment1);
            if(fragment1.endsWith("\\")) {
                buff.delete(buff.length() - 1, buff.length());
                buff.append(splitStr);
            } else {
                fragmentList.add(buff.toString());
                buff.delete(0, buff.length());
            }
        }

        StringBuilder var9 = new StringBuilder();

        for(Iterator var10 = fragmentList.iterator(); var10.hasNext(); var9.append(',')) {
            String var12 = (String)var10.next();
            if(!isNumber) {
                var9.append('\'');
                var9.append(var12);
                var9.append('\'');
            } else {
                var9.append(var12);
            }
        }

        if(var9.length() > 0) {
            var9.delete(var9.length() - 1, var9.length());
        }

        String var11 = var9.toString();
        if(var11.trim().isEmpty()) {
            var11 = null;
        }

        return var11;
    }


}
