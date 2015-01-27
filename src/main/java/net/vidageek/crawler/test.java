package net.vidageek.crawler;

import net.vidageek.crawler.component.LinkNormalizer.impl.DefaultLinkNormalizer;
import net.vidageek.crawler.component.LinkNormalizer.LinkNormalizer;


/**
 * Created by kingjci on 15-1-25.
 */
public class test {
    public static void main(String args[]){
        LinkNormalizer normalizer= new DefaultLinkNormalizer("http://www.baidu.com/");
        String str = normalizer.normalize("http://www.baidu.com/dfasdf/");
        System.out.println(str);

    }
}
