package com.itheima.hchat.service.impl;

import com.itheima.hchat.mapper.TbFriendMapper;
import com.itheima.hchat.mapper.TbFriendReqMapper;
import com.itheima.hchat.mapper.TbUserMapper;
import com.itheima.hchat.pojo.*;
import com.itheima.hchat.pojo.vo.Result;
import com.itheima.hchat.pojo.vo.User;
import com.itheima.hchat.service.UserService;
import com.itheima.hchat.util.FastDFSClient;
import com.itheima.hchat.util.IdWorker;
import com.itheima.hchat.util.QRCodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author 拾柒
 * @create 2020/1/8
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private TbUserMapper userMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private FastDFSClient fastDFSClient;
    @Autowired
    private Environment env;
    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private TbFriendMapper friendMapper;
    @Autowired
    private TbFriendReqMapper friendReqMapper;

    @Override
    public List<TbUser> findAll() {
        return userMapper.selectByExample(null);
    }

    /**
     * 登录
     * @param username
     * @param password
     * @return
     */
    @Override
    public User login(String username, String password) {
        if (StringUtils.isNotBlank(username)&&StringUtils.isNotBlank(password)){
            TbUserExample example=new TbUserExample();
            TbUserExample.Criteria criteria = example.createCriteria();
            criteria.andUsernameEqualTo(username);

            List<TbUser> userList=userMapper.selectByExample(example);
            if (userList!=null&&userList.size()==1){
                //对密码进行校验
                String encodingPassword = DigestUtils.md5DigestAsHex(password.getBytes());
                if (encodingPassword.equals(userList.get(0).getPassword())){
                    User user=new User();
                    BeanUtils.copyProperties(userList.get(0),user);
                    return user;
                }

            }
        }
        return null;
    }

    /**
     * 用户注册
     * @param user
     */
    @Override
    public void register(TbUser user) {
        try {
            //1.判断这个用户名是否存在
            TbUserExample example=new TbUserExample();
            TbUserExample.Criteria criteria = example.createCriteria();


            criteria.andUsernameEqualTo(user.getUsername());

            List<TbUser> userList = userMapper.selectByExample(example);
            if (userList!=null&&userList.size()>0){
                throw new RuntimeException("用户已存在");
            }

            //2.将用户信息保存到数据库中
            user.setId(idWorker.nextId());
            //对密码进行加密
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
            user.setPicNormal("");
            user.setNickname(user.getUsername());

            //生成二维码，并且将二维码的路径保存到数据库中
            //要生成的二维码中的字符串
            String qrcodeStr="hichat://"+user.getUsername();
            //获取一个临时目录，用来保存临时的二维码图片
            String tempDir = env.getProperty("hcat.tmpdir");
            String qrCodeFilePath=tempDir+user.getUsername()+".png";
            qrCodeUtils.createQRCode(qrCodeFilePath, qrcodeStr);


            // 将临时保存的二维码上传到FastDFS
            String url = env.getProperty("fdfs.httpurl") +
                    fastDFSClient.uploadFile(new File(qrCodeFilePath));

            user.setQrcode(url);
            user.setCreatetime(new Date());

            userMapper.insert(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("注册失败");
        }
    }

    /**
     * 上传用户图片
     * @param file
     * @param userid
     * @return
     */
    @Override
    public User upload(MultipartFile file, String userid) {
        try {
            //返回在FastDfs中的URL，这个路径是不带http://192.168.25.133/...
            String url = fastDFSClient.uploadFace(file);
            System.out.println(url);

            //在FastDFS上传的时候，会自动生成一个缩略图
            //文件名_150*150.后缀
            String[] fileNameList=url.split("\\.");
            String fileName=fileNameList[0];
            String ext=fileNameList[1];

            String picSmallUrl=fileName+"_150x150."+ext;

            String prefix=env.getProperty("fdfs.httpurl");
            TbUser tbUser = userMapper.selectByPrimaryKey(userid);

            //设置图像大图片
            tbUser.setPicNormal(prefix+url);
            //设置图像小图片
            tbUser.setPicSmall(prefix+picSmallUrl);
            //将新的头像URL更新到数据中
            userMapper.updateByPrimaryKey(tbUser);

            //将用户信息返回到Controller
            User user=new User();
            BeanUtils.copyProperties(tbUser,user);
            return user;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void updateNickname(String id, String nickname) {
        if (StringUtils.isNotBlank(nickname)){
            TbUser tbUser = userMapper.selectByPrimaryKey(id);
            tbUser.setNickname(nickname);
            userMapper.updateByPrimaryKey(tbUser);
        }else{
            throw new RuntimeException("昵称不能为空");
        }
    }

    @Override
    public User findById(String userid) {
        TbUser tbUser = userMapper.selectByPrimaryKey(userid);
        User user=new User();
        BeanUtils.copyProperties(tbUser,user);
        return user;
    }

    @Override
    public User findByUsername(String userid, String friendUsername) {

        TbUserExample example=new TbUserExample();
        TbUserExample.Criteria criteria = example.createCriteria();

        criteria.andUsernameEqualTo(friendUsername);

        List<TbUser> userList = userMapper.selectByExample(example);
        TbUser friend=userList.get(0);

        User friendUser=new User();
        BeanUtils.copyProperties(friend,friendUser);
        return friendUser;
    }


    /**
     * 检查是否允许添加好友
     * @param userid 要添加好友的用户id
     * @param friend 好友的用户名
     */
    private void checkAllowToAddFriend(String userid,TbUser friend){
        //1.用户不能添加自己为好友
        if (friend.getId().equals(userid)){
            throw new RuntimeException("不能添加自己为好友");
        }


        //2.用户不能重复添加
        //如果用户已经添加该好友，就不能再次添加
        TbFriendExample friendExample=new TbFriendExample();
        TbFriendExample.Criteria friendCriteria= friendExample.createCriteria();

        friendCriteria.andUseridEqualTo(userid);
        friendCriteria.andFriendsIdEqualTo(friend.getId());

        List<TbFriend> friendList = friendMapper.selectByExample(friendExample);
        if ((friendList!=null&&friendList.size()>0)){
            throw  new RuntimeException(friend.getUsername()+"已经是您的好友了");
        }

        //3.判断是否已经提交好友申请，如果已经提交好友申请，就直接抛出运行时异常
        TbFriendReqExample friendReqExample=new TbFriendReqExample();
        TbFriendReqExample.Criteria friendReqCriteria = friendReqExample.createCriteria();

        //当前用户发送的好友请求
        friendReqCriteria.andFromUseridEqualTo(userid);
        friendReqCriteria.andToUseridEqualTo(friend.getId());
        //而且这个请求是没有被处理的
        friendReqCriteria.andStatusEqualTo(0);
        List<TbFriendReq> friendReqList = friendReqMapper.selectByExample(friendReqExample);
        if (friendReqList!=null&&friendReqList.size()>0){
            throw  new  RuntimeException("已经申请过了");
        }

    }
}
