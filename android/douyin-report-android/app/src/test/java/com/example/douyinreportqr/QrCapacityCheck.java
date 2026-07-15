package com.example.douyinreportqr;

public class QrCapacityCheck {
    public static void main(String[] args) throws Exception {
        String longDeepLink = "snssdk1128://webview?refer=web&from=webview&from_ssr=1&from_aid=1128&app=aweme&scene_from=share_reflow&host=www.iesdouyin.com&browser_name=safari&is_edenx=1&forbid_pasteboard=1&gd_label=click_schema_ug_filter_v1_click_schema_lhft_48148317a&launch_h5_method=click_wap_rf_video_report&url=https%3A%2F%2Fapi.amemv.com%2Ffalcon%2Ffe_douyin_security_react%2Fmortise%2F9879902d-907f-46f3-85e0-678c416f366a%2F%3Freport_type%3Dnote%26object_id%3D7654811955592898930%26sec_owner_id%3DMS4wLjABAAAArdo4ql4bGt7Wfdyvr1N_qtKw5ad0coSlSGuXznCaPjE%26enter_from%3Daweme_reflow%26hide_nav_bar%3D1%26should_full_screen%3D1&hide_nav_bar=1&should_full_screen=1&enter_from=aweme_reflow";
        SimpleQr.encode(longDeepLink, 900);
        System.out.println("qr capacity check passed");
    }
}
