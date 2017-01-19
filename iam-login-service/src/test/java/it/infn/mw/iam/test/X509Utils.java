package it.infn.mw.iam.test;

import java.util.ArrayList;
import java.util.List;

public class X509Utils {

  public static final List<X509Cert> x509Certs = new ArrayList<X509Cert>();

  static {

    X509Cert x509Cert = new X509Cert();
    x509Cert.display = "Personal Certificate";
    x509Cert.certificate = "MIIEWDCCA0CgAwIBAgIDAII4MA0GCSqGSIb3DQEBCwUAMC4xCzAJBgNVBAYTAklU"
        + "MQ0wCwYDVQQKEwRJTkZOMRAwDgYDVQQDEwdJTkZOIENBMB4XDTE1MDUxODEzNTQx"
        + "NFoXDTE2MDUxNzEzNTQxNFowZDELMAkGA1UEBhMCSVQxDTALBgNVBAoTBElORk4x"
        + "HTAbBgNVBAsTFFBlcnNvbmFsIENlcnRpZmljYXRlMQ0wCwYDVQQHEwRDTkFGMRgw"
        + "FgYDVQQDEw9FbnJpY28gVmlhbmVsbG8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw"
        + "ggEKAoIBAQDf74gCX/5D7HAKlI9u+vMy4R8uYvtZp60L401zOuDHc0sKPCq2sU8N"
        + "IB8cNOC+69h+hPqbU8gcleXZ0T3KOy3NPrU7CFaOxzsCVAoDcLeKFlCMu4X1OK0V"
        + "NPq7+fgJ1cVdsJ4StHl3oTtQPCoU6NNly8HJIufVjat2IgjNHdMHINs5IcxpTmE5"
        + "OGae3reOfRBtqBr8UvyiTwHEEll6JpdbKjzjrcHBoOdFZTiwR18fO+B8MZLOjXSk"
        + "OEG5p5K8y4UOkHQeqooKgW0tn7dvCxQfuu5TGYUmK6pwjcxzcnSE9U4abFh5/oD1"
        + "PqjoCGtlvnl9nGrhAFD+qa5zq6SrgWsNAgMBAAGjggFHMIIBQzAMBgNVHRMBAf8E"
        + "AjAAMA4GA1UdDwEB/wQEAwIEsDAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUH"
        + "AwQwPQYDVR0fBDYwNDAyoDCgLoYsaHR0cDovL3NlY3VyaXR5LmZpLmluZm4uaXQv"
        + "Q0EvSU5GTkNBX2NybC5kZXIwJQYDVR0gBB4wHDAMBgorBgEEAdEjCgEHMAwGCiqG"
        + "SIb3TAUCAgEwHQYDVR0OBBYEFIQEiwCbKssJqSBNMziZtu54ZQRCMFYGA1UdIwRP"
        + "ME2AFNFi87N3csgu+/J5Gm83TiefE9UgoTKkMDAuMQswCQYDVQQGEwJJVDENMAsG"
        + "A1UEChMESU5GTjEQMA4GA1UEAxMHSU5GTiBDQYIBADAnBgNVHREEIDAegRxlbnJp"
        + "Y28udmlhbmVsbG9AY25hZi5pbmZuLml0MA0GCSqGSIb3DQEBCwUAA4IBAQBfhv9P"
        + "4bYo7lVRYjHrxreKVaEyujzPZFowZPYMz0e/lPcdqh9TIoDBbhy7/PXiTVqQEniZ"
        + "fU1Nso4rqBj8Qy609Y60PEFHhfLnjhvd/d+pXu6F1QTzUMwA2k7z5M+ykh7L46/z"
        + "1vwvcdvCgtWZ+FedvLuKh7miTCfxEIRLcpRPggbC856BSKet7jPdkMxkUwbFa34Z"
        + "qOuDQ6MvcrFA/lLgqN1c1OoE9tnf/uyOjVYq8hyXqOAhi2heE1e+s4o3/PQsaP5x"
        + "LetVho/J33BExHo+hCMt1rN89DO5qU7FFijLlbmOZROacpjkPNn2V4wkd5WeX2dm" + "b6UoBRqPsAiQL0mY";
    x509Certs.add(x509Cert);

    x509Cert = new X509Cert();
    x509Cert.display = "Personal Certificate";
    x509Cert.certificate = "MIIDnjCCAoagAwIBAgIBCTANBgkqhkiG9w0BAQUFADAtMQswCQYDVQQGEwJJVDE"
        + "MMAoGA1UECgwDSUdJMRAwDgYDVQQDDAdUZXN0IENBMB4XDTEyMDkyNjE1MzkzNF"
        + "oXDTIyMDkyNDE1MzkzNFowKzELMAkGA1UEBhMCSVQxDDAKBgNVBAoTA0lHSTEOM"
        + "AwGA1UEAxMFdGVzdDAwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDK"
        + "xtrwhoZ27SxxISjlRqWmBWB6U+N/xW2kS1uUfrQRav6auVtmtEW45J44VTi3WW6"
        + "Y113RBwmS6oW+3lzyBBZVPqnhV9/VkTxLp83gGVVvHATgGgkjeTxIsOE+TkPKAo"
        + "ZJ/QFcCfPh3WdZ3ANI14WYkAM9VXsSbh2okCsWGa4o6pzt3Pt1zKkyO4PW0cBkl"
        + "etDImJK2vufuDVNm7Iz/y3/8pY8p3MoiwbF/PdSba7XQAxBWUJMoaleh8xy8HSR"
        + "On7tF2alxoDLH4QWhp6UDn2rvOWseBqUMPXFjsUi1/rkw1oHAjMroTk5lL15GI0"
        + "LGd5dTVopkKXFbTTYxSkPz1MLAgMBAAGjgcowgccwDAYDVR0TAQH/BAIwADAdBg"
        + "NVHQ4EFgQUfLdB5+jO9LyWN2/VCNYgMa0jvHEwDgYDVR0PAQH/BAQDAgXgMD4GA"
        + "1UdJQQ3MDUGCCsGAQUFBwMBBggrBgEFBQcDAgYKKwYBBAGCNwoDAwYJYIZIAYb4"
        + "QgQBBggrBgEFBQcDBDAfBgNVHSMEGDAWgBSRdzZ7LrRp8yfqt/YIi0ojohFJxjA"
        + "nBgNVHREEIDAegRxhbmRyZWEuY2VjY2FudGlAY25hZi5pbmZuLml0MA0GCSqGSI"
        + "b3DQEBBQUAA4IBAQANYtWXetheSeVpCfnId9TkKyKTAp8RahNZl4XFrWWn2S9We"
        + "7ACK/G7u1DebJYxd8POo8ClscoXyTO2BzHHZLxauEKIzUv7g2GehI+SckfZdjFy"
        + "RXjD0+wMGwzX7MDuSL3CG2aWsYpkBnj6BMlr0P3kZEMqV5t2+2Tj0+aXppBPVwz"
        + "JwRhnrSJiO5WIZAZf49YhMn61sQIrepvhrKEUR4XVorH2Bj8ek1/iLlgcmFMBOd"
        + "s+PrehSRR8Gn0IjlEgC68EY6KPE+FKySuS7Ur7lTAjNdddfdAgKV6hJyST6/dx8"
        + "ymIkb8nxCPnxCcT2I2NvDxcPMc/wmnMa+smNal0sJ6m";
    x509Certs.add(x509Cert);
  }

}
