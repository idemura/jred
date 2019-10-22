package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Protocol {
    private Protocol() {}

    public static final class Repo {
        private String name;
        private String revision;

        public Repo() {}

        public Repo(String name, String revision) {
            this.name = name;
            this.revision = revision;
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        @JsonProperty("revision")
        public String getRevision() {
            return revision;
        }
    }

    public static final class Copy {
        private Repo repo;
        private String file;
        private String data;

        public Copy() {}

        public Copy(Repo repo, String file, String data) {
            this.repo = repo;
            this.file = file;
            this.data = data;
        }

        @JsonProperty("repo")
        public Repo getRepo() {
            return repo;
        }

        @JsonProperty("file")
        public String getFile() {
            return file;
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

        public Diff(Repo repo, String diff) {
            this.repo = repo;
            this.diff = diff;
        }

        @JsonProperty("repo")
        public Repo getRepo() {
            return repo;
        }

        @JsonProperty("diff")
        public String getDiff() {
            return diff;
        }
    }

    public static final class Error {
        private String message;

        public Error() {
            this("");
        }

        public Error(String message) {
            this.message = message;
        }

        @JsonProperty("message")
        public String getMessage() {
            return message;
        }
    }
}
