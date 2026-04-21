package com.videosite.backend.intelligence.llm;

import java.util.List;

public class LlmMessage {

    private String role;
    private Object content;

    public LlmMessage() {
    }

    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public LlmMessage(String role, List<ContentPart> contentParts) {
        this.role = role;
        this.content = contentParts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public static class ContentPart {
        private String type;
        private String text;
        private ImageUrl imageUrl;

        public ContentPart() {
        }

        public static ContentPart text(String text) {
            ContentPart part = new ContentPart();
            part.type = "text";
            part.text = text;
            return part;
        }

        public static ContentPart imageUrl(String url) {
            ContentPart part = new ContentPart();
            part.type = "image_url";
            part.imageUrl = new ImageUrl(url);
            return part;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public ImageUrl getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(ImageUrl imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class ImageUrl {
        private String url;

        public ImageUrl() {
        }

        public ImageUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
