package org.example;

/**
 * 字符串大整数加法的小示例类。
 * 这个类与 Drools 主流程无关，主要用于演示基础算法代码。
 */
public class add {
    /**
     * 对两个仅包含数字字符的字符串进行逐位相加。
     *
     * @param a 加数 A（字符串形式）
     * @param b 加数 B（字符串形式）
     * @return 加法结果（字符串形式）
     */
    public static String addScore(String a, String  b){
        // i/j 从末尾开始向前移动，用于模拟人工竖式加法
        int i = a.length()-1;
        int j = b.length()-1;
        // c 表示进位（carry）
        int c=0;
        StringBuilder sb = new StringBuilder();

        // 只要任一数字尚未处理完，或还有进位，就继续计算
        while(i>=0 || j >=0 || c>0){
            int numA = 0;
            if(i >=0){
                // 将字符数字转换为整数：'7' - '0' == 7
                numA = a.charAt(i) - '0';
                i --;
            }
            int numB = 0;
            if(j >=0){
                // 读取 B 的当前位
                numB = b.charAt(i) - '0';
                j --;
            }
            // 当前位求和 = A位 + B位 + 上一位进位
            int sum = numA + numB  + c ;
            // 更新下一位进位
            c = sum/10;
            // 当前位结果
            int r = sum%10;
            // 先追加低位，最终再 reverse
            sb.append(r);
        }
        // 由于从低位到高位追加，结果需要反转
        return sb.reverse().toString();
    }

    /**
     * 本地快速演示入口。
     */
    public static void main(String[] args) {
        String a = "5";
        String b = "10";
        String sum = addScore(a, b);
        System.out.println("The sum of " + a + " and " + b + " is: " + sum);
    }
}
