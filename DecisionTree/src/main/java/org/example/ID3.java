package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ID3 {
    public ArrayList<String> attribute = new ArrayList<String>(); // имена атрибутов
    public ArrayList<ArrayList<String>> attributevalue = new ArrayList<ArrayList<String>>(); // значения атрибутов
    public ArrayList<String[]> data = new ArrayList<String[]>();; // строка данных
    public int decatt; // индекс вычисляемого атрибута
    public static final String patternString = "@attribute(.*)[{](.*?)[}]";

    public Document xmlDoc;
    public Element root;

    public ID3() {
        xmlDoc = DocumentHelper.createDocument();
        root = xmlDoc.addElement("root");
        root.addElement("DecisionTree").addAttribute("value", "null");
    }

    public void readData(File file) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            Pattern pattern = Pattern.compile(patternString);
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    attribute.add(matcher.group(1).trim());
                    String[] values = matcher.group(2).split(",");
                    ArrayList<String> al = new ArrayList<String>(values.length);
                    for (String value : values) {
                        al.add(value.trim());
                    }
                    attributevalue.add(al);
                } else if (line.startsWith("@data")) {
                    while ((line = br.readLine()) != null) {
                        if(line=="")
                            continue;
                        String[] row = line.split(",");
                        data.add(row);
                    }
                } else {
                    continue;
                }
            }
            br.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setDecisionValue(int n) {
        if (n < 0 || n >= attribute.size()) {
            System.err.println("Вычисляемый атрибут выбран некорректно.");
            System.exit(2);
        }
        decatt = n;
    }

    public void setDecisionValue(String name) {
        int n = attribute.indexOf(name);
        setDecisionValue(n);
    }

    public double calculateEntropy(int[] arr) {
        double entropy = 0.0;
        int sum = 0;
        for (int j : arr) {
            entropy -= j * Math.log(j + Double.MIN_VALUE) / Math.log(2);
            sum += j;
        }
        entropy += sum * Math.log(sum+Double.MIN_VALUE)/Math.log(2);
        entropy /= sum;
        return entropy;
    }

    public double calculateEntropy(int[] arr, int sum) {
        double entropy = 0.0;
        for (int i = 0; i < arr.length; i++) {
            entropy -= arr[i] * Math.log(arr[i]+Double.MIN_VALUE)/Math.log(2);
        }
        entropy += sum * Math.log(sum+Double.MIN_VALUE)/Math.log(2);
        entropy /= sum;
        return entropy;
    }

    public boolean infoPure(ArrayList<Integer> subset) {
        String value = data.get(subset.get(0))[decatt];
        for (int i = 1; i < subset.size(); i++) {
            String next=data.get(subset.get(i))[decatt];
            if (!value.equals(next))
                return false;
        }
        return true;
    }

    public double calculateNodeEntropy(ArrayList<Integer> subset, int index) {
        int sum = subset.size();
        double entropy = 0.0;
        int[][] info = new int[attributevalue.get(index).size()][];
        for (int i = 0; i < info.length; i++)
            info[i] = new int[attributevalue.get(decatt).size()];
        int[] count = new int[attributevalue.get(index).size()];
        for (int i = 0; i < sum; i++) {
            int n = subset.get(i);
            String nodeValue = data.get(n)[index];
            int nodeInd = attributevalue.get(index).indexOf(nodeValue);
            count[nodeInd]++;
            String decValue = data.get(n)[decatt];
            int decInd = attributevalue.get(decatt).indexOf(decValue);
            info[nodeInd][decInd]++;
        }
        for (int i = 0; i < info.length; i++) {
            entropy += calculateEntropy(info[i]) * count[i] / sum;
        }
        return entropy;
    }

    public void buildTree(String name, String value, ArrayList<Integer> subset,
                        LinkedList<Integer> selatt) {
        Node ele = null;
        @SuppressWarnings("unchecked")
        List<Node> list = root.selectNodes("//"+name);
        Iterator<Node> iter= list.iterator();
        Element el = null;
        while(iter.hasNext()){
            ele=iter.next();
            el = (Element) ele;
            if(el.attributeValue("value").equals(value))
                break;
        }
        if (infoPure(subset)) {
            el.setText(data.get(subset.get(0))[decatt]);
            return;
        }
        int minIndex = -1;
        double minEntropy = Double.MAX_VALUE;
        for (int i = 0; i < selatt.size(); i++) {
            if (i == decatt)
                continue;
            double entropy = calculateNodeEntropy(subset, selatt.get(i));
            if (entropy < minEntropy) {
                minIndex = selatt.get(i);
                minEntropy = entropy;
            }
        }
        String nodeName = attribute.get(minIndex);
        selatt.remove(Integer.valueOf(minIndex));
        ArrayList<String> attValues = attributevalue.get(minIndex);
        for (String val : attValues) {
            el.addElement(nodeName).addAttribute("value", val);
            ArrayList<Integer> al = new ArrayList<Integer>();
            for (Integer integer : subset) {
                if (data.get(integer)[minIndex].equals(val)) {
                    al.add(integer);
                }
            }
            buildTree(nodeName, val, al, selatt);
        }
    }

    public void writeXML(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file);
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter output = new XMLWriter(fw, format);
            output.write(xmlDoc);
            output.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
