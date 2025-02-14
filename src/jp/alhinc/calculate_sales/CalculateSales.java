package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String COMMODITY_NAME_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String COMMODITY_NAME_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String SHOP_FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String COMMODITY_FILE_NOT_EXIST = "商品定義ファイルが存在しません";
	private static final String SHOP_FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SEQUENTIAL = "売上ファイル名が連番になっていません";
	private static final String THE_NUMBER_EXEEDS_THE_LIMIT = "合計金額が10桁を超えました";
	private static final String SHOP_CODE_INVALID = "の支店コードが不正です";
	private static final String COMMODITY_CODE_INVALID = "の商品コードが不正です";
	private static final String INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// エラー処理
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales,"^[0-9]{3}$", SHOP_FILE_INVALID_FORMAT, SHOP_FILE_NOT_EXIST)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], COMMODITY_NAME_LST, commodityNames, commoditySales,"^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8}$", COMMODITY_FILE_INVALID_FORMAT, COMMODITY_FILE_NOT_EXIST)) {
			return;
		}
		
		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);

		// 売り上げファイル連番確認のためのエラー処理
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println(FILE_NOT_SEQUENTIAL);
				return;
			}
		}

		BufferedReader br = null;

		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				List<String> salesList = new ArrayList<String>();

				while((line = br.readLine()) != null) {
					salesList.add(line);
				}

				// 売り上げファイル定義に合っているかどうかの確認エラー処理
				if(salesList.size() != 3) {
					System.out.println(rcdFiles.get(i).getName() + INVALID_FORMAT);
					return;
				}

				// 支店定義ファイルに売り上げファイルの主キーが存在しているかの確認処理エラー処理
				if (!branchNames.containsKey(salesList.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + SHOP_CODE_INVALID);
					return;
				}
				
				// 商品定義ファイルに売り上げファイルの主キーが存在しているかの確認処理エラー処理
				if (!commodityNames.containsKey(salesList.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + COMMODITY_CODE_INVALID);
					return;
				}
				
				// 売上金額が数字かどうか確認するためのエラー処理
				if(!salesList.get(2).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(salesList.get(2));
				Long shopSaleAmount = branchSales.get(salesList.get(0)) + fileSale;
				Long commoditySaleAmount = commoditySales.get(salesList.get(1)) + fileSale;
				
				// 支店別集計ファイルの桁数オーバー確認エラー処理
				if(shopSaleAmount >= 10000000000L){
					System.out.println(THE_NUMBER_EXEEDS_THE_LIMIT);
					return;
				}
				
				// 商品別集計ファイルの桁数オーバー確認エラー処理
				if(commoditySaleAmount >= 10000000000L){
					System.out.println(THE_NUMBER_EXEEDS_THE_LIMIT);
					return;
				}

				branchSales.put(salesList.get(0), shopSaleAmount);
				commoditySales.put(salesList.get(1), commoditySaleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if(br != null) {
					try {
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		
		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], COMMODITY_NAME_OUT, commodityNames, commoditySales)) {
			return;
		}
	}


	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales, String regularExpresssion, String invalidMessage, String notExistMessege) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			// 支店および商品定義ファイルの存在チェックのためのエラー処理
			if(!file.exists()) {
				System.out.println(notExistMessege);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");

				// エラー処理
				if((items.length != 2) || (!items[0].matches(regularExpresssion))){
				    //支店および商品定義ファイルの仕様が満たされていない場合、
				    //エラーメッセージをコンソールに表示します。
					System.out.println(invalidMessage);
					return false;
				}

				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}