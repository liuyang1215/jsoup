package jsoup;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.com.ly.Item;

public class TestJsoup {
	private static final Logger log = Logger.getLogger(TestJsoup.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
 
	
	//得到有效的商品分类(2600多个分类，有效1200多个分类)
	public List<String> getAllCat(String url) throws IOException {
		List<String> catList = new ArrayList<String>();
		//抓取网页
		Document document = Jsoup.connect(url).get();
		String html = document.body().html();		
		//System.out.println(html);
		
	// 获取a标签的href属性
	//	Elements eles = document.getAllElements();
		Elements eles = document.select(".items .clearfix dd a");
		for(Element els : eles) {
			String href = els.attr("href");
	//		System.out.println(href);
			catList.add("http:" + href);
		}
		return catList;
	}

	@Test
	public void run() throws IOException {
//		String url = "https://list.jd.com/list.html?cat=9987,653,659";
//		Integer pageNum = this.getPageNum(url);
//		System.out.println(pageNum);
		
		//分类页面
//		String url = "https://www.jd.com/allSort.aspx";
//		List<String> catPageList = this.getAllCatPage(url);
//		for(String catPageUrl : catPageList) {
//			System.out.println(catPageUrl);
//		}
//		System.out.println(catPageList.size());
//		String url = "http://list.jd.com/list.html?cat=12379,13302,13311&page=3";
//     	this.getItem(url);
		
		String url = "http://item.jd.com/2342601.html";
		System.out.println(this.getItemInfo(url));
	}
	//获取某个分类的页数
	public Integer getPageNum(String url) throws IOException {
		try {
			String s = Jsoup.connect(url).get().select("#J_topPage .fp-text i").get(0).text();
			return Integer.parseInt(s);
		 } catch (Exception e) {
			log.error(e.getMessage());
			return 0;
		}
	}
	
	//获取所有分类所有的页链接
	public List<String> getAllCatPage(String url) throws IOException {
		List<String> catPageList = new ArrayList<String>();
		List<String> catList = this.getAllCat(url);
		for(String catUrl : catList) {
			//获取当前分类的页数
			Integer pageNum = this.getPageNum(catUrl);
			// 遍历所有的页
			for(int i = 1 ; i <= pageNum ; i++) {
				//获取分类下每页链接
				String pageUrl = catUrl + "&page = " + i;
				catPageList.add(pageUrl);
			}
			System.out.println(catUrl + "-" + pageNum);
		}
		return catPageList;
	}
	
	//获取某个分类某页的一个所有商品的链接
	public List<String> getItem(String url) throws IOException {
		List<String> itemList = new ArrayList<String>();
		try {
			//css是多个样式时，多个select
		Elements eles = Jsoup.connect(url).get().select(".gl-i-wrap").select(".j-sku-item .p-img a");
		for(Element ele  : eles) {
			//某个商品的链接
			String itemUrl = "http:" + ele.attr("href");
			System.out.println(itemUrl);
			itemList.add(itemUrl);
		}
		return itemList;
		}catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}
	
	//获取商品数据
	public Item getItemInfo(String url) throws IOException {
		Item item = new Item();
		Long id = Long.parseLong (url.replace("http://item.jd.com/", "").replace(".html", ""));
		item.setId(id);
		Document doc = Jsoup.connect(url).get();
		//商品标题
		String title = doc.select("title").get(0).text();
		item.setTitle(title);
		
		//获取价格	
		String priceUrl = "http://p.3.cn/prices/mgets?skuIds=J_" + id;
		String jsonPrice = Jsoup.connect(priceUrl).ignoreContentType(true).execute().body();
		JsonNode priceJsonNode = MAPPER.readTree(jsonPrice).get(0).get("p");
		Long price = (long) (priceJsonNode.asDouble()*100);
		item.setPrice(price);
		
		//获取商品的卖点
		String sellPointUrl = "http://ad.3.cn/ads/mgets?skuids=AD_" + id;
		String jsonSellPoint = Jsoup.connect(sellPointUrl).ignoreContentType(true).execute().body();
		String sellPoint = MAPPER.readTree(jsonSellPoint).get(0).get("ad").asText();
		item.setSellPoint(sellPoint);
		
		//获取商品的图片
		String image = "";
		Elements eles = doc.select("#spec-img");
		for(Element ele : eles) {
			image += "http:"+ele.attr("data-origin")+",";
		}
		if(image.length() > 0) {
			image = image.substring(0, image.length() - 1); //删除最后一个逗号
		}
		item.setImage(image);
		
		//获取商品详情，返回jsonp
		String descUrl = "http://d.3.cn/desc/" + id;
		String jsonpDesc = Jsoup.connect(descUrl).ignoreContentType(true).execute().body();
		String s = jsonpDesc.replace("showdesc(", "");
		String jsonDesc = s.substring(0,s.length() - 1);
		String desc = MAPPER.readTree(jsonDesc).get("content").asText();
		item.setItemDesc(desc);
			
		return item;
 	}
}
