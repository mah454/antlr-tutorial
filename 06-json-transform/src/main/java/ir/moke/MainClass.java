package ir.moke;

public class MainClass {
    private static final String jsonData = """
            [
              {
                "username": "aaa",
                "password": "1234",
                "profile": {
                  "name": "Ali",
                  "family": "Mohammadi",
                  "age": 45,
                  "account": 9287311,
                  "contact": {
                    "city": "Tehran",
                    "zip": 12345
                  },
                  "address" : [
                    {"country" : [ "OZ", "IT", "IR"]},
                    {"state" :  "Tehran"},
                    {"state" :  "Hormozgan"}
                  ]
                }
              },
              {
                "username": "bbb",
                "password": "1234",
                "profile": {
                  "name": "Mahdi",
                  "family": "Sheikh Hosseini",
                  "age": 21,
                  "account": 12456789794,
                  "contact": {
                    "city": "Pardis",
                    "zip": 99999
                  },
                  "address" : [
                    {"country" : [ "AF", "AR", "EN"]},
                    {"state" :  "Mazanderan"},
                    {"state" :  "Khorasan"}
                  ]
                }
              },
              {
                "username": "ccc",
                "password": "1234",
                "profile": {
                  "name": "Hossein",
                  "family": "Javadi",
                  "age": 33,
                  "account": 11111111111111,
                  "contact": {
                    "city": "Eghlid",
                    "zip": 4444
                  },
                  "address" : [
                    {"country" : [ "IR", "JP", "EN"]},
                    {"state" :  "Golestan"},
                    {"state" :  "Zanjan"}
                  ]
                }
              }
            ]
            """;

    static void main() {
        /* Check Filter */
        JsonStream.of(jsonData)
//                .filter("filter -> username = \"aaa\"")
//                .filter("filter -> username = \"aaa\" or username = \"bbb\"")
//                .filter("filter -> profile.address[ state = \"Zanjan\" ]")
//                .filter("filter -> [].profile.address[ state = \"Zanjan\" or state = \"Tehran\"]")
//                .filter("filter -> profile.address[0]")
//                .filter("filter -> [].profile.address[0]")
//                .filter("filter -> [].profile.name = \"ali\"")
//                .filter("filter -> [0]")
//                .filter("filter -> profile.address[].state == \"Hormozgan\"")
//                .filter("filter -> [].profile.address[].country[0] = \"IR\"")
                .filter("filter -> profile.address[].country[ @ == \"IR\" or @ == \"OZ\" ]")
                .prettyPrint();


        /* Check Map */
//        JsonStream.of(jsonData)
//                .map("map -> profile.name = \"jafar\"")
//                .map("map -> profile.age = profile.age * 3 ")
//                .map("map -> profile.age = profile.age + \" \" + 1111")
//                .map("map -> profile.name = null")
//                .map("map -> profile.contact.fullName = profile.name + \" \" + profile.family")
//                .map("[].profile.address[ state == \"Golestan\" ].zz = \"hello\" ")
//                .prettyPrint();
    }
}
