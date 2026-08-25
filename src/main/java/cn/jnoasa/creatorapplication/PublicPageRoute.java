package cn.jnoasa.creatorapplication;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class PublicPageRoute {
    @Bean
    RouterFunction<ServerResponse> creatorApplicationPage() {
        return route().GET("/creator/apply", request -> ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML).bodyValue(HTML)).build();
    }

    private static final String HTML = """
        <!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>创作者申请</title>
        <style>:root{font-family:Inter,"Microsoft YaHei",sans-serif}*{box-sizing:border-box}body{margin:0;min-height:100vh;background:#f3f5f9;color:#172033;display:grid;place-items:center;padding:24px}.card{width:min(680px,100%);background:#fff;border:1px solid #e7eaf0;border-radius:20px;padding:30px;box-shadow:0 18px 55px #1b2b4b18}h1{margin:0 0 8px;font-size:28px}.sub{color:#667085;margin:0 0 24px}.tabs{display:flex;gap:8px;margin-bottom:20px}.tabs button{flex:1}.field{margin:15px 0}label{display:block;font-weight:600;margin-bottom:7px}input,textarea,button{font:inherit}input,textarea{width:100%;border:1px solid #d4d9e2;border-radius:10px;padding:11px;background:#fff;color:#172033}textarea{min-height:110px;resize:vertical}button{border:0;border-radius:10px;padding:11px 16px;cursor:pointer}.tabs button{background:#eef1f6;color:#475467}.tabs .active,.submit{background:#2859d9;color:#fff}.submit{width:100%;font-weight:700;margin-top:8px}.msg{min-height:22px;margin-top:14px;color:#2859d9}.hide{display:none}.hint{font-size:13px;color:#667085}</style></head>
        <body><main class="card"><h1>创作者申请</h1><p class="sub">成为创作者分两步完成。昵称将自动使用你当前登录的 Halo 账号。</p><div class="tabs"><button id="t1" class="active">第一步 · 申请投稿</button><button id="t2">第二步 · 申请发布</button></div>
        <form id="form"><section id="s1"><div class="field"><label>QQ 群成员截图</label><input id="shot" type="file" accept="image/png,image/jpeg,image/webp" required><p class="hint">仅供管理员审核；最大 2 MB。</p></div><div class="field"><label>申请理由</label><textarea id="reason" maxlength="1000" required></textarea></div></section><section id="s2" class="hide"><div class="field"><label>文章名称</label><input id="title" maxlength="200"></div><div class="field"><label>文章链接</label><input id="url" type="url" placeholder="https://jnoasa.cn/archives/..."></div></section><button class="submit" type="submit">提交申请</button><div id="msg" class="msg"></div></form></main>
        <script>let stage='CONTRIBUTOR',data=null;const q=s=>document.querySelector(s);q('#t1').onclick=()=>tab('CONTRIBUTOR');q('#t2').onclick=()=>tab('AUTHOR');function tab(x){stage=x;q('#s1').classList.toggle('hide',x!=='CONTRIBUTOR');q('#s2').classList.toggle('hide',x!=='AUTHOR');q('#t1').classList.toggle('active',x==='CONTRIBUTOR');q('#t2').classList.toggle('active',x==='AUTHOR')}q('#shot').onchange=e=>{const f=e.target.files[0];if(!f)return;if(f.size>2*1024*1024){alert('截图不能超过 2 MB');e.target.value='';return}const r=new FileReader;r.onload=()=>data=r.result;r.readAsDataURL(f)};q('#form').onsubmit=async e=>{e.preventDefault();q('#msg').textContent='正在提交…';const body={stage,reason:q('#reason').value,qqScreenshot:data,articleTitle:q('#title').value,articleUrl:q('#url').value};try{const r=await fetch('/apis/api.creator.jnoasa.cn/v1alpha1/applications',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});if(r.status===401||r.status===403){location.href='/login?redirect_uri='+encodeURIComponent(location.href);return}if(!r.ok){const x=await r.json().catch(()=>({}));throw new Error(x.detail||x.message||'提交失败')}q('#msg').textContent='申请已提交，请等待管理员审核。';q('#form').reset();data=null}catch(err){q('#msg').textContent=err.message}}</script></body></html>
        """;
}
