package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Protocol {
    private Protocol() {}

    public static final class Repo {
        private String name;
        private String revision;

        public Repo() {}

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        @JsonProperty("revision")
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

        @JsonProperty("repo")
        public Repo getRepo() {
            return repo;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        @JsonProperty("file_name")
        public String getFileName() {
            return fileName;
        }

        public void setData(String data) {
            this.data = data;
        }

        @JsonProperty("data")
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

        @JsonProperty("repo")
        public Repo getRepo() {
            return repo;
        }

        public void setDiff(String diff) {
            this.diff = diff;
        }

        @JsonProperty("diff")
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

        @JsonProperty("error")
        public int getError() {
            return error;
        }

        @JsonProperty("details")
        public String getDetails() {
            return details;
        }
    }
}
