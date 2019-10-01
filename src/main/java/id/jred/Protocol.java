package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Protocol {
    private Protocol() {}

    public static final String MIME_JSON = "application/json";
    public static final String MIME_TEXT = "text/plain; charset=utf-8";

    public static final Status OK = new Status();

    public static final class Repo {
        private String name;
        private String revision;

        public Repo() {}

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty
        public String getName() {
            return name;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        @JsonProperty
        public String getRevision() {
            return revision;
        }
    }

    public static final class Copy {
        private Repo repo;
        private String fileName;
        private String data;

        public Copy() {}

        public void setRepo(Repo repo) {
            this.repo = repo;
        }

        @JsonProperty
        public Repo getRepo() {
            return repo;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        @JsonProperty
        public String getFileName() {
            return fileName;
        }

        public void setData(String data) {
            this.data = data;
        }

        @JsonProperty
        public String getData() {
            return data;
        }
    }

    public static final class Diff {
        private Repo repo;
        private String diff;

        public Diff() {}

        public void setRepo(Repo repo) {
            this.repo = repo;
        }

        @JsonProperty
        public Repo getRepo() {
            return repo;
        }

        public void setDiff(String diff) {
            this.diff = diff;
        }

        @JsonProperty
        public String getDiff() {
            return diff;
        }
    }

    public static final class Status {
        private int error = 200;
        private String details;

        public Status() {}

        public Status(int error, String details) {
            this.error = error;
            this.details = details;
        }

        public Status(int error, Throwable cause) {
            this(error, cause.toString());
        }

        @JsonProperty
        public int getError() {
            return error;
        }

        @JsonProperty
        public String getDetails() {
            return details;
        }
    }
}
