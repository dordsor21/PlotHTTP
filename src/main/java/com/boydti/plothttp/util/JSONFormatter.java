package com.boydti.plothttp.util;

import java.util.Iterator;

import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONException;
import com.intellectualcrafters.json.JSONObject;

public class JSONFormatter {

    public static String format(final JSONObject object) throws JSONException {
        final JsonVisitor visitor = new JsonVisitor(4, ' ');
        visitor.visit(object, 0);
        return visitor.toString();
    }

    private static class JsonVisitor {

        private final StringBuilder builder = new StringBuilder();
        private final int indentationSize;
        private final char indentationChar;

        public JsonVisitor(final int indentationSize, final char indentationChar) {
            this.indentationSize = indentationSize;
            this.indentationChar = indentationChar;
        }

        private void visit(final JSONArray array, final int indent) throws JSONException {
            final int length = array.length();
            if (length == 0) {
                write("[]", indent);
            } else {
                write("[", indent);
                for (int i = 0; i < length; i++) {
                    visit(array.get(i), indent + 1);
                }
                write("]", indent);
            }

        }

        private void visit(final JSONObject obj, final int indent) throws JSONException {
            final int length = obj.length();
            if (length == 0) {
                write("{}", indent);
            } else {
                write("{", indent);
                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    write(key + " :", indent + 1);
                    visit(obj.get(key), indent + 1);
                    if (keys.hasNext()) {
                        write(",", indent + 1);
                    }
                }
                write("}", indent);
            }

        }

        private void visit(final Object object, final int indent) throws JSONException {
            if (object instanceof JSONArray) {
                visit((JSONArray) object, indent);
            } else if (object instanceof JSONObject) {
                visit((JSONObject) object, indent);
            } else {
                if (object instanceof String) {
                    write("\"" + (String) object + "\"", indent);
                } else {
                    write(String.valueOf(object), indent);
                }
            }

        }

        private void write(final String data, final int indent) {
            for (int i = 0; i < (indent * this.indentationSize); i++) {
                this.builder.append(this.indentationChar);
            }
            this.builder.append(data).append('\n');
        }

        @Override
        public String toString() {
            return this.builder.toString();
        }

    }

}
