package com.dataline.BajajPortal.convert;

// import java.text.DecimalFormat;


public class EnglishNumberToWords {

    public static void main(String arg[]) {
        EnglishNumberToWords ss = new EnglishNumberToWords();
        System.out.println(ss.convert(121921.87));
    }


    public String convert(Double num1) {
        Long num = num1.longValue();
        String NumbetToString = "";

        Double decimal = roundTwoDecimals((num - num1) * 100);
        Long decimalpont = decimal.longValue();
        Long digittonumber = Long.valueOf(num);
        String digitalValue = digittonumber.toString();

        if (digitalValue.length() == 9) {
            NumbetToString = ConvertNineDigitNumber(num);
        }

        if (digitalValue.length() == 8) {
            NumbetToString = convertEightDigitNumber(num);
        }
        if (digitalValue.length() == 7) {
            NumbetToString = convertSevenDigitNumber(num);
        }

        if (digitalValue.length() == 6) {
            NumbetToString = convertSixDigitNumber(num);
        }

        if (digitalValue.length() == 5) {
            NumbetToString = converFiveDigitNumber(num);
        }

        if (digitalValue.length() == 4) {
            NumbetToString = converFourDigitNumber(num);
        }

        if (digitalValue.length() == 3) {
            NumbetToString = ConverThreeDigitNumber(num);
        }

        if (digitalValue.length() == 2) {
            NumbetToString = converTowDigitNumber(num);
        }
        if (digitalValue.length() == 1) {
            NumbetToString = ConverOneDigitNumber(num);
        }
        Long decimal1 = Long.valueOf(0);
        if (decimalpont < 0) {
            String de = decimalpont.toString();
            de = de.substring(1);
            decimal1 = Long.parseLong(de);
        }
        String nn = NumbetToString + " Rupees And" + converTowDigitNumber(decimal1) + " Paise Only";
        return nn;
    }

    public String ConvertNineDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 10000000;
        NumbetToString = converTowDigitNumber(s);
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            if (s > 20) {
                s = s % 10;
            }
            NumbetToString += ConverOneDigitNumber(s);
            NumbetToString += " Karod";
        }

        long p = n;
        p = p % 10000000;
        NumbetToString += convertSevenDigitNumber(p);
        return NumbetToString;

    }

    public String convertEightDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 10000000;

        NumbetToString = ConverOneDigitNumber(s);
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            NumbetToString += " Karod";
        }
        long p = n;
        p = p % 10000000;
        NumbetToString += convertSevenDigitNumber(p);
        return NumbetToString;
    }

    public String convertSevenDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 100000;

        NumbetToString = converTowDigitNumber(s);
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            if (s > 20) {
                s = s % 10;
            }
            NumbetToString += ConverOneDigitNumber(s);
            NumbetToString += " Lakhs";
        }
        long p = n;
        p = p % 100000;
        NumbetToString += converFiveDigitNumber(p);

        return NumbetToString;
    }

    public String convertSixDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 100000;
        NumbetToString = ConverOneDigitNumber(s);

        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            NumbetToString += " Lakhs";
        }
        long p = n;
        p = p % 100000;
        NumbetToString += converFiveDigitNumber(p);

        return NumbetToString;
    }

    public String converFiveDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 1000;
        NumbetToString = converTowDigitNumber(s);
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            if (s > 20) {
                s = s % 10;
            }
            NumbetToString += " Thousand ";
        }
        long p = n;
        p = p % 1000;
        NumbetToString += ConverThreeDigitNumber(p);

        return NumbetToString;
    }

    public String converFourDigitNumber(Long n) {
        String NumbetToString = "";
        long s = n;
        s = s / 1000;
        NumbetToString = ConverOneDigitNumber(s).trim();
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            NumbetToString += " Thousand ";
        }
        long p = n;
        p = p % 1000;
        NumbetToString += ConverThreeDigitNumber(p).trim();

        return NumbetToString;
    }

    public String ConverThreeDigitNumber(Long n) {

        String NumbetToString = "";
        long s = n;
        s = s / 100;
        NumbetToString = ConverOneDigitNumber(s).trim();
        String calulats = NumbetToString;
        if (calulats.length() == 0) {
            NumbetToString += "";
        } else {
            NumbetToString += " Hundred";
        }
        long p = n;
        p = p % 100;
        String TodigitConversionString = converTowDigitNumber(p);
        NumbetToString += TodigitConversionString;
        return NumbetToString;
    }

    public String ConverOneDigitNumber(Long n) {
        String NumbetToString = "";
        if (n == 1) {
            NumbetToString += " One";
        }
        if (n == 2) {
            NumbetToString += " Two";
        }
        if (n == 3) {
            NumbetToString += " Three";
        }
        if (n == 4) {
            NumbetToString += " Four";
        }
        if (n == 5) {
            NumbetToString += " Five";
        }
        if (n == 6) {
            NumbetToString += " Six";
        }
        if (n == 7) {
            NumbetToString += " Seven";
        }
        if (n == 8) {
            NumbetToString += " Eight";
        }
        if (n == 9) {
            NumbetToString += " Nine";
        }

        return NumbetToString;
    }

    public String converTowDigitNumber(Long n) {
        String OneDegit = n.toString();
        String NumbetToString = "";
        if (n == 0) {
            NumbetToString += " Zero";
        }
        if (n == 10) {
            NumbetToString += " Ten";
        }
        if (n == 11) {
            NumbetToString += " Eleven";
        }
        if (n == 12) {
            NumbetToString += " Twelve";
        }
        if (n == 13) {
            NumbetToString += " Thirteen";
        }
        if (n == 14) {
            NumbetToString += " Fourteen";
        }
        if (n == 15) {
            NumbetToString += " Fifteen";
        }
        if (n == 16) {
            NumbetToString += " Sixteen";
        }
        if (n == 17) {
            NumbetToString += " Seventeen";
        }
        if (n == 18) {
            NumbetToString += " Eighteen";
        }
        if (n == 19) {
            NumbetToString += " Nineteen";
        }
        if (n >= 20 && n <= 29) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Twenty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 30 && n <= 39) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Thirty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 40 && n <= 49) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Forty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 50 && n <= 59) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Fifty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 60 && n <= 69) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Sixty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 70 && n <= 79) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Seventy" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 80 && n <= 89) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Eighty" + ConverOneDigitNumber(onedegit);
        }
        if (n >= 90 && n <= 99) {
            Long onedegit = Long.parseLong(OneDegit.substring(1));
            NumbetToString += " Ninety" + ConverOneDigitNumber(onedegit);
        }
        if (n < 10 && n >= 1) {
            NumbetToString += ConverOneDigitNumber(n);
        }
        return NumbetToString;
    }

    public double roundTwoDecimals(double d) {
        java.text.DecimalFormat twoDForm = new java.text.DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }
}