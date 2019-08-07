package com.mulesoft.aws.secrets.manager.provider.api;

public enum RegionsEnum {

    Mumbai{
        public String toString() {
            return "ap-south-1";
        }
    }, Seoul{
        public String toString() {
            return "ap-northeast-2";
        }
    }, Singapore{
        public String toString() {
            return "ap-southeast-1";
        }
    }, Sydney{
        public String toString() {
            return "ap-southeast-2";
        }
    }, Tokyo{
        public String toString() {
            return "ap-northeast-1";
        }
    }, Canada{
        public String toString() {
            return "ca-central-1";
        }
    }, Frankfurt{
        public String toString() {
            return "eu-central-1";
        }
    }, Ireland{
        public String toString() {
            return "eu-west-1";
        }
    }, London{
        public String toString() {
            return "eu-west-2";
        }
    }, Paris{
        public String toString() {
            return "eu-west-3";
        }
    }, SaoPaulo{
        public String toString() {
            return "sa-east-1";
        }
    }, NCalifornia{
        public String toString() {
            return "us-west-1";
        }
    }, Oregon{
        public String toString() {
            return "us-west-2";
        }
    }, NVirginia{
        public String toString() {
            return "us-east-1";
        }
    }, Ohio{
        public String toString() {
            return "us-east-2";
        }
    }

}
