package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) {
        ID3 inst = new ID3();
        inst.readData(new File("input/patients.arff"));
        inst.setDecisionValue("hospitalize");

        LinkedList<Integer> ll=new LinkedList<Integer>();
        for(int i=0;i<inst.attribute.size();i++){
            if(i!=inst.decatt)
                ll.add(i);
        }
        ArrayList<Integer> al=new ArrayList<Integer>();
        for(int i=0;i<inst.data.size();i++){
            al.add(i);
        }

        inst.buildTree("DecisionTree", "null", al, ll);
        inst.writeXML("input/hospitalize_result.xml");
    }
}