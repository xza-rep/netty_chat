package com.itheima.hchat.service;

import com.itheima.hchat.pojo.TbUser;
import com.itheima.hchat.pojo.vo.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 拾柒
 * @create 2020/1/8
 */
public interface UserService {

    /**
     * 返回数据库中的所有用户
     * @return
     */
    List<TbUser> findAll();

    /**
     * 用来登录检查，检查用户名密码是否正确
     * @param username
     * @param password
     * @return
     */
    User login(String username , String password);

    /**
     * 用户注册
     * @param user
     */
    void register(TbUser user);

    /**
     * 上传图像
     * @param file
     * @param userid
     * @return
     */
    User upload(MultipartFile file, String userid);

    /**
     * 根据用户id更新用户昵称
     * @param id
     * @param nickname
     */
    void updateNickname(String id, String nickname);

    /**
     * 重新加载用户
     * @param userid
     * @return
     */
    User findById(String userid);

    /**
     * 根据用户名搜索用户（好友搜索）
     * 在搜索用户的时候不要验证
     * @param userid 用户id
     * @param friendUsername 好友的用户名
     * @return 如果搜索到好友，就返回用户对象，否则返回null
     */
    User findByUsername(String userid, String friendUsername);
}
