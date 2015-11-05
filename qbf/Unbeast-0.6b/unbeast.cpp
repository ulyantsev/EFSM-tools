// Unbeast v.0.6 - (C) 2009/2010 by Ruediger Ehlers
// The tool is free for use in the academic context. Furthermore, the licensing details as stated in the document at the following URL apply: http://react.cs.uni-saarland.de/tools/caissa/LICENSE
//
//See the enclosed README file for details on compiling.
#include <string>
#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>
#include <vector>
#include <set>
#include <errno.h>
#include <sstream>
#include <iostream>
#include <stdio.h>
#include <assert.h>
#include <cudd.h>
#include <cuddInt.h>
#include <boost/utility.hpp>
#include <list>
#include <cstdlib>
#include <map>
#include <cassert>
#include <libxml/tree.h>
#include <libxml/parser.h>
#include <libxml/xpath.h>
#include <libxml/xpathInternals.h>
#include <string>
#include <sstream>
#include <vector>
#include <algorithm>
#include <assert.h>
#include <fstream>
#include <dddmp.h>
#include <limits>
#include <stdlib.h>
#include <stdint.h>
#include <tr1/unordered_map>
#include <boost/algorithm/string.hpp>
#include <libxml/xpointer.h>
#include <tr1/unordered_set>
#include <unistd.h>
#include <algorithm>
#include <termios.h>
#undef fail // for CUDD

class _s_2 {
protected:
    std::string _s_3;
public:
    _s_2(const std::string &msg) : _s_3(msg) {};
    virtual ~_s_2() {};
    _s_2(const char* file, int line) {
        std::ostringstream s;
        s << "Assertion failed: " << file << ", line " << line;
        _s_3 = s.str();
    }
    const std::string _s_19() const {
        return _s_3;
    };
};
class _s_4 : public _s_2 {
private:
    static std::string _s_5(const std::string &msg, int line) {
        std::ostringstream stream;
        stream << "Exception in XML input file (line " << line << "): " << msg;
        return stream.str();
    }
public:
    _s_4(const std::string &msg, int line) : _s_2(_s_5(msg,line)) {};
};
class _s_6 {
private:
    FILE *_s_7;
    boost::iostreams::stream<boost::iostreams::file_descriptor_source> *_s_8;
public:
    _s_6(const char *_s_442) : _s_8(NULL) {
        _s_7 = popen(_s_442, "r");
        if (_s_7 == NULL) throw _s_2("Cannot call the System description compiler");
        _s_8 = new boost::iostreams::stream<boost::iostreams::file_descriptor_source>(fileno(_s_7),boost::iostreams::never_close_handle);
    }
    boost::iostreams::stream<boost::iostreams::file_descriptor_source> *_s_9() {
        return _s_8;
    }
    ~_s_6() {
        if (_s_7!=NULL) {
            if (pclose(_s_7)==-1) {
                if (errno==ECHILD) throw "Cannot obtain child status: pclose";
                throw "pclose: Unknown errno";
            }
        }
        if (_s_8!=NULL) delete _s_8;
    }
};
class _s_10 {
private:
    char _s_11[100];
    std::vector<std::string> _s_12;
public:
    _s_10() {
        strncpy(_s_11, "/tmp/synthesis.XXXXXX", sizeof _s_11);
        if (mkdtemp(_s_11) == NULL)
            throw _s_2("Unable to retrieve temporary file name");
    }
    ~_s_10() {
        for (std::vector<std::string>::iterator it = _s_12.begin(); it!=_s_12.end(); it++) {
            if (remove((*it).c_str())==-1)
                std::cout << "Cannot delete temporary file: " << (*it) << std::endl;
        }
        if (remove(_s_11)==-1)
            throw _s_2(std::string("Cannot delete temporary directory: ")+_s_11);
    }
    std::string _s_13() {
        return _s_11;
    }
    std::string _s_14(const char *name) {
        std::string theFile = std::string(_s_11)+"/"+name;
        _s_12.push_back(theFile);
        return theFile;
    }
    void _s_15(std::string const &name) {
        _s_12.push_back(name);
    }
};
inline void _s_16(std::vector<uint> &_s_126, const std::vector<uint> &src) {
    for (std::vector<uint>::const_iterator it = src.begin(); it!=src.end(); it++) {
        _s_126.push_back(*it);
    }
}
inline void _s_17(std::vector<std::string> &_s_126, const std::vector<std::string> &src) {
    for (std::vector<std::string>::const_iterator it = src.begin(); it!=src.end(); it++) {
        _s_126.push_back(*it);
    }
}
class _s_18 {
    std::string _s_3;
public:
    _s_18(const std::string &msg) : _s_3(msg) {};
    virtual ~_s_18() {};
    const std::string _s_19() const {
        return _s_3;
    };
};
inline void _s_20(std::istream &in, const std::string &_s_142) {
    char *c = new char[_s_142.length()+1];
    in.get(c,_s_142.length()+1);
    std::cout.flush();
    if (c!=_s_142) {
        delete[] c;
        std::ostringstream msg;
        msg << "Expectation Failure, expected: '" << _s_142 << "' but found '" <<c<<"'.";
        throw _s_18(msg.str());
    }
    delete[] c;
}
template<typename A> void _s_21(std::vector<A> &_s_126, const std::vector<A> &src) {
    for (uint i=0; i<src.size(); i++) {
        _s_126.push_back(src[i]);
    }
}
template<typename A> void _s_22(std::vector<A> &_s_126, const std::vector< std::vector<A> > &src) {
    for (uint i=0; i<src.size(); i++) {
        _s_21(_s_126,src[i]);
    }
}
template<typename A> void _s_22(std::vector<A> &_s_126, const std::vector< std::vector<std::vector< A> > > &src) {
    for (uint i=0; i<src.size(); i++) {
        _s_22(_s_126,src[i]);
    }
}
inline void _s_23(std::vector<uint> &_s_126, const std::vector< std::vector<std::vector< uint> > > &src) {
    _s_22<uint>(_s_126,src);
}
template <class T>
inline static size_t _s_24(char delim, const char* source, T &_s_126)
{
    std::string _s_25;
    std::istringstream iss(source);
    size_t i = 0;
    while (getline(iss, _s_25, delim)) {
        _s_126.push_back(_s_25);
        ++i;
    }
    return i;
}
template <typename T> inline static std::string _s_26(std::set<T> const &u) {
    std::ostringstream os;
    typedef typename std::set<T>::const_iterator Iterator;
    bool first = true;
    os << "{";
    for (Iterator it = u.begin(); it!=u.end(); it++) {
        if (first) {
            first = false;
        } else {
            os << ",";
        }
        os << *it;
    }
    os << "}";
    return os.str();
}
class _s_27;
class BFBddManager;
class _s_28;
class _s_29;
class BFBddManager : boost::noncopyable {
private:
    DdManager *mgr;
    int _s_30;
    bool _s_31;
    void _s_32(bool _s_611);
public:
    BFBddManager(uint maxMemoryInMB = 3096, float reorderingMaxBlowup = 1.2f);
    ~BFBddManager();
    inline _s_27 _s_33();
    inline _s_27 _s_34();
    inline _s_27 _s_35();
    _s_28 _s_36(const _s_27 * vars, const int * _s_37, int n);
    _s_28 _s_36(const std::vector<_s_27> &vars);
    _s_29 _s_38(const std::vector<_s_27> &vars);
    inline _s_27 _s_39(const std::vector<_s_27> &_s_95);
    inline _s_27 _s_40(const std::vector<_s_27> &_s_95);
    void _s_41(const std::vector<_s_27> &_s_612);
    void _s_42(bool _s_611);
    void _s_43();
    void _s_44();
    void _s_45();
    void _s_46();
    bool _s_47(std::string _s_48, std::vector<_s_27> &_s_49);
    bool _s_50(std::string _s_48, std::vector<_s_27> &_s_49);
    bool _s_47(FILE *file, std::vector<_s_27> &_s_49);
    bool _s_50(FILE *file, std::vector<_s_27> &_s_49);
    DdManager *_s_610() const {
        return mgr;
    };
    friend _s_29 _s_51(const std::vector<_s_27> &_s_52, BFBddManager *mgr );
    friend class _s_27;
    friend class _s_28;
    friend class _s_29;
};
class _s_27 {
    friend class BFBddManager;
    friend class _s_28;
    friend class _s_29;
private:
    DdManager *mgr;
    DdNode *node;
public:
    inline _s_27(DdManager *_s_53, DdNode *_s_54) : mgr(_s_53), node(_s_54) {
        Cudd_Ref(node);
    };
    inline _s_27() : node(NULL) {};
    inline ~_s_27() {
        if (node!=NULL) Cudd_RecursiveDeref(mgr,node);
    };
    inline _s_27(const _s_27 &bdd) : mgr(bdd.mgr), node(bdd.node) {
        if (node!=NULL) Cudd_Ref(node);
    };
    inline bool _s_55() const {
        return node==(Cudd_Not(Cudd_ReadOne(mgr)));
    };
    inline bool _s_56() const {
        return node==Cudd_ReadOne(mgr);
    };
    inline double _s_57() {
        return Cudd_CountPathsToNonZero(node);
    }
    double _s_58(const _s_28 &cube) const;
    inline bool isValid() const {
        return node!=NULL;
    };
    inline _s_27 operator&=(const _s_27& _s_127) {
        {}
        DdNode *_s_59 = Cudd_bddAnd(mgr, node, _s_127.node);
        Cudd_Ref(_s_59);
        Cudd_RecursiveDeref(mgr,node);
        node = _s_59;
        {}
        return *this;
    };
    inline _s_27 operator|=(const _s_27& _s_127) {
        {}
        assert(mgr==_s_127.mgr);
        DdNode *_s_59 = Cudd_bddOr(mgr, node, _s_127.node);
        Cudd_Ref(_s_59);
        Cudd_RecursiveDeref(mgr,node);
        node = _s_59;
        {}
        return *this;
    };
    inline int operator==(const _s_27& _s_127) const {
        return (node==_s_127.node);
    };
    inline _s_27& operator=(const _s_27 &_s_127) {
        if (this==&_s_127) return *this;
        if (node!=NULL) Cudd_RecursiveDeref(mgr,node);
        node = _s_127.node;
        mgr = _s_127.mgr;
        if (node!=NULL) Cudd_Ref(node);
        return *this;
    }
    inline int operator<=(const _s_27& _s_127) const {
        return Cudd_bddLeq(mgr,node,_s_127.node);
    };
    inline int operator>=(const _s_27& _s_127) const {
        return Cudd_bddLeq(mgr,_s_127.node,node);
    };
    inline int operator<(const _s_27& _s_127) const {
        int value = Cudd_bddLeq(mgr,node,_s_127.node) && (node!=_s_127.node);
        return value;
    };
    inline int operator>(const _s_27& _s_127) const {
        return Cudd_bddLeq(mgr,_s_127.node,node) && (node!=_s_127.node);
    };
    inline _s_27 operator!() const {
        assert(node!=0);
        return _s_27(mgr,Cudd_Not(node));
    };
    inline _s_27 operator&(const _s_27& _s_127) const {
        return _s_27(mgr,Cudd_bddAnd(mgr,node,_s_127.node));
    };
    inline _s_27 operator|(const _s_27& _s_127) const {
        return _s_27(mgr,Cudd_bddOr(mgr,node,_s_127.node));
    };
    inline _s_27 operator-(const _s_27& _s_127) const {
        return _s_27(mgr,Cudd_bddAnd(mgr,node,Cudd_Not(_s_127.node)));
    };
    inline _s_27 operator^(const _s_27& _s_127) const {
        return _s_27(mgr,Cudd_bddXor(mgr,node,_s_127.node));
    };
    inline int operator!=(const _s_27& _s_127) const {
        return node!=_s_127.node;
    };
    BFBddManager *_s_60() const;
    inline bool _s_63(const _s_27 &_s_61) const {
        return *this==_s_61;
    }
    inline bool _s_64(const _s_27 &_s_61) const {
        return *this==_s_61;
    }
    inline _s_27 _s_65(const _s_27 &_s_127) const {
        DdNode *_s_541 = Cudd_bddLICompaction(mgr,node,_s_127.node);
        return _s_27(mgr,_s_541);
    }
    inline bool _s_66() const {
        return Cudd_IsConstant(node);
    };
    inline int _s_67() const {
        return Cudd_DagSize(node);
    };
    inline unsigned int _s_68() const {
        return Cudd_NodeReadIndex(node);
    };
    inline _s_27 _s_69(const _s_29 &x, const _s_29 &y) const;
    inline _s_27 _s_70(const _s_27& g, const _s_28& cube) const;
    inline _s_27 _s_71(const _s_28& cube) const;
    inline _s_27 _s_72(const _s_27& var) const;
    inline _s_27 _s_73(const _s_28& cube) const;
    inline _s_27 _s_74(const _s_27& _s_127) const {
        return !(*this) | _s_127;
    };
    inline DdNode* _s_608() const {
        return node;
    };
    _s_27 _s_75(const _s_28& cube) const;
    void _s_75(const _s_28& cube, std::list<std::pair<DdHalfWord,bool> > &_s_126) const;
    uint _s_609() const {
        return Cudd_ReadInvPerm(mgr,Cudd_NodeReadIndex(node));
    }
    inline size_t _s_76() const {
        return (size_t)(node);
    };
    inline _s_27 _s_613() const {
        if (Cudd_IsComplement(node)) {
            return _s_27(mgr,Cudd_Not(Cudd_T(node)));
        }
        return _s_27(mgr,Cudd_T(node));
    }
    inline _s_27 _s_614() const {
        if (Cudd_IsComplement(node)) {
            return _s_27(mgr,Cudd_Not(Cudd_E(node)));
        }
        return _s_27(mgr,Cudd_E(node));
    }
    class _s_77 : public boost::noncopyable {
    private:
        DdGen *gen;
        int *cube;
        BFBddManager *mgr;
    public:
        typedef enum {_s_78 = 1,_s_79=0,_s_80=2} _s_81;
        _s_77(_s_27 &bdd);
        ~_s_77();
        inline operator bool() const {
            if (gen==NULL) return false;
            if (Cudd_IsGenEmpty(gen)) {
                return false;
            }
            return true;
        };
        inline _s_81 operator[](unsigned int position) const {
            return (_s_81)(cube[position]);
        }
        inline _s_77& operator++(int) {
            CUDD_VALUE_TYPE value;
            (void)Cudd_NextCube(gen, &cube, &value);
            return *this;
        };
        inline _s_77& operator++() {
            CUDD_VALUE_TYPE value;
            (void)Cudd_NextCube(gen, &cube, &value);
            return *this;
        };
    };
};
class _s_28 {
    friend class _s_27;
    friend class BFBddManager;
private:
    DdNode *cube;
    DdManager *mgr;
    int _s_82;
    _s_28( DdManager *_s_53, DdNode *node, int size ) : cube(node), mgr(_s_53), _s_82(size) {
        Cudd_Ref(cube);
    };
public:
    _s_28() : cube(NULL) {};
    _s_28(const _s_28 &_s_127) : cube(_s_127.cube), mgr(_s_127.mgr), _s_82(_s_127._s_82) {
        if (cube!=NULL) Cudd_Ref(cube);
    };
    _s_28(const _s_27 &onlyOne) : cube(onlyOne.node), mgr(onlyOne.mgr), _s_82(1) {
        if (cube!=NULL) Cudd_Ref(cube);
    };
    ~_s_28() {
        if (cube!=NULL) Cudd_RecursiveDeref(mgr,cube);
    };
    int size() const {
        return _s_82;
    };
    _s_28 &operator=(const _s_28 &_s_127) {
        if (this==&_s_127) return *this;
        if (cube!=NULL) Cudd_RecursiveDeref(mgr,cube);
        cube = _s_127.cube;
        mgr = _s_127.mgr;
        _s_82 = _s_127._s_82;
        if (cube!=NULL) Cudd_Ref(cube);
        return *this;
    };
};
class _s_29 {
private:
    DdNode **_s_83;
    DdManager *mgr;
    int nofNodes;
    friend class _s_27;
    friend class BFBddManager;
public:
    _s_29() : _s_83(NULL) {};
    ~_s_29() {
        if (_s_83!=NULL) {
            for (int i=0; i<nofNodes; i++) Cudd_RecursiveDeref(mgr,_s_83[i]);
            delete[] _s_83;
        }
    }
    _s_29(_s_29 const &_s_127) {
        nofNodes = _s_127.nofNodes;
        mgr = _s_127.mgr;
        if (_s_127._s_83!=NULL) {
            _s_83 = new DdNode*[nofNodes];
            for (int i=0; i<nofNodes; i++) {
                DdNode *temp = _s_127._s_83[i];
                Cudd_Ref(temp);
                _s_83[i] = temp;
            }
        } else {
            _s_83 = NULL;
        }
    }
    _s_29& operator=(const _s_29 &_s_127) {
        if (this==&_s_127) return *this;
        if (_s_83!=NULL) {
            for (int i=0; i<nofNodes; i++) Cudd_RecursiveDeref(mgr,_s_83[i]);
            delete[] _s_83;
        }
        nofNodes = _s_127.nofNodes;
        mgr = _s_127.mgr;
        if (_s_127._s_83!=NULL) {
            _s_83 = new DdNode*[nofNodes];
            for (int i=0; i<nofNodes; i++) {
                DdNode *temp = _s_127._s_83[i];
                Cudd_Ref(temp);
                _s_83[i] = temp;
            }
        } else {
            _s_83 = NULL;
        }
        return *this;
    }
};
inline _s_27 BFBddManager::_s_33() {
    return _s_27(mgr,Cudd_ReadOne(mgr));
};
inline _s_27 BFBddManager::_s_34() {
    return _s_27(mgr,Cudd_Not(Cudd_ReadOne(mgr)));
};
inline _s_27 BFBddManager::_s_35() {
    return _s_27(mgr,Cudd_bddNewVar(mgr));
};
inline _s_27 _s_27::_s_69(const _s_29 &x, const _s_29 &y) const {
    return _s_27(mgr,Cudd_bddSwapVariables(mgr,node,x._s_83,y._s_83,x.nofNodes));
}
inline _s_27 _s_27::_s_70(const _s_27& g, const _s_28& cube) const {
    return _s_27(mgr,Cudd_bddAndAbstract(mgr,node,g.node,cube.cube));
}
inline _s_27 _s_27::_s_71(const _s_28& cube) const {
    return _s_27(mgr,Cudd_bddExistAbstract(mgr,node,cube.cube));
}
inline _s_27 _s_27::_s_72(const _s_27& var) const {
    (void)var;
    return _s_71(_s_28(var));
}
inline _s_27 _s_27::_s_73(const _s_28& cube) const {
    return _s_27(mgr,Cudd_bddUnivAbstract(mgr,node,cube.cube));
}
inline _s_27 BFBddManager::_s_39(const std::vector<_s_27> &_s_95) {
    DdNode *_s_96 = Cudd_ReadOne(mgr);
    Cudd_Ref(_s_96);
    for (std::vector<_s_27>::const_iterator it = _s_95.begin(); it!=_s_95.end(); it++) {
        DdNode *next = Cudd_bddAnd(mgr,_s_96,it->node);
        Cudd_Ref(next);
        Cudd_RecursiveDeref(mgr,_s_96);
        _s_96 = next;
    }
    Cudd_Deref(_s_96);
    return _s_27(mgr,_s_96);
}
inline _s_27 BFBddManager::_s_40(const std::vector<_s_27> &_s_95) {
    DdNode *_s_96 = Cudd_Not(Cudd_ReadOne(mgr));
    Cudd_Ref(_s_96);
    for (std::vector<_s_27>::const_iterator it = _s_95.begin(); it!=_s_95.end(); it++) {
        DdNode *next = Cudd_bddOr(mgr,_s_96,it->node);
        Cudd_Ref(next);
        Cudd_RecursiveDeref(mgr,_s_96);
        _s_96 = next;
    }
    Cudd_Deref(_s_96);
    return _s_27(mgr,_s_96);
}
class _s_84 {
public:
    virtual ~_s_84() {};
    virtual void _s_85(std::vector<std::string> &_s_92) const = 0;
    virtual void _s_86(std::string type, std::vector<uint> &_s_93) const = 0;
    virtual _s_27 _s_87(uint _s_94) const = 0;
    virtual std::string _s_88(uint _s_94) const = 0;
};
void _s_89(const _s_84 &cont, const _s_27 &b, const char* _s_90, const std::string _s_48);
class _s_91 {
protected:
    std::string _s_3;
public:
    _s_91(const std::string &msg) : _s_3(msg) {};
    virtual ~_s_91() {};
    _s_91(const char* file, int line) {
        std::ostringstream s;
        s << "BFDumpDotException thrown: " << file << ", line " << line;
        _s_3 = s.str();
    }
    const std::string _s_19() const {
        return _s_3;
    };
};
class _s_97 {
public:
    virtual BFBddManager &_s_98() = 0;
    virtual _s_27 _s_99(std::string name) const = 0;
    virtual void _s_100(std::vector<std::string> &vars) {
        vars.size();
        throw "BddNBA_getListOfVars is Unsupported";
    };
};
class _s_101 {
private:
    void _s_102(_s_27 _s_142, std::vector<std::string> &_s_103, uint _s_104, std::string _s_105, std::string _s_106, std::ostringstream &_s_107) const;
protected:
    std::vector<std::string> _s_108;
    std::vector<bool> _s_109;
    std::map<std::pair<uint,uint>,_s_27 > _s_110;
    _s_97 *_s_111;
public:
    _s_101() : _s_111(0) {};
    _s_101(_s_97 *_s_115) : _s_111(_s_115) {};
    virtual ~_s_101() {};
    uint _s_112() const;
    uint _s_113(const std::string &name, bool _s_109);
    void _s_114(uint _s_52, std::string to, _s_27 _s_116);
    void _s_114(uint _s_52, uint to, _s_27 _s_116);
    uint _s_117(const std::string &name) const;
    std::string _s_118(uint _s_94) const;
    bool _s_119(uint _s_120) const;
    void _s_121();
    void _s_122(_s_101 &_s_59) const;
    bool _s_123() const;
    bool _s_124() const;
    void _s_125(const _s_101 &_s_127, _s_101 &_s_126) const;
    void _s_128(std::set<uint> &_s_129) const;
    void _s_130();
    bool _s_131(uint _s_132) const;
    void _s_133();
    void _s_134(_s_101 &_s_126) const;
    void _s_135(const _s_101 &_s_136, _s_101 &_s_137, bool _s_138) const;
    _s_27 _s_139(uint _s_52, uint to) const;
    std::string _s_140() const;
    void _s_1(std::vector<bool> &_s_59) const;
    _s_27 _s_141(const std::string _s_142);
    static void _s_143(_s_101 &_s_144, std::string _s_145);
};
class _s_146 {
private:
    xmlDocPtr _s_147;
    xmlXPathContextPtr _s_148;
public:
    class _s_149 {
    private:
        xmlXPathObjectPtr _s_150;
        void free();
    public:
        _s_149();
        ~_s_149();
        void _s_167(const std::string &_s_152, xmlXPathContextPtr _s_151);
        xmlNodePtr operator[](size_t index) const;
        size_t size() const;
        bool empty() const;
    };
    _s_146();
    _s_146(const std::string &_s_153);
    _s_146(const std::string &_s_154, const std::string &_s_153);
    virtual ~_s_146();
    void _s_155(const std::string &_s_153) const;
    void _s_156(std::string &_s_157) const;
    xmlNodePtr _s_158(const std::string &name);
    xmlNodePtr _s_159(xmlNodePtr _s_161, const std::string &name);
    xmlNodePtr _s_159(xmlNodePtr _s_161, const std::string &name, const
                      std::string &value);
    void _s_160(xmlNodePtr node, const std::string &value);
    xmlNodePtr _s_160(const std::string &_s_152, const std::string &value);
    void _s_162(xmlNodePtr node, const std::string &name, const std::string &value);
    void _s_163(xmlNodePtr n);
    xmlNodePtr _s_164(const std::string &_s_152) const;
    xmlNodePtr _s_164(xmlNodePtr _s_165, const std::string &_s_152) const;
    bool _s_166(const std::string &_s_152) const;
    void _s_167(const std::string &_s_152, _s_149 &_s_59) const;
    void _s_168(const std::string &_s_152, _s_149 &_s_59, const xmlNodePtr _s_165) const;
    std::string _s_169(const std::string &_s_152) const;
    std::string _s_169(xmlNodePtr _s_165, const std::string &_s_152) const;
    static std::string _s_170(xmlNodePtr node);
    static std::string _s_171(xmlNodePtr node);
    static std::string _s_172(xmlNodePtr node, const std::string &name);
};
enum _s_173 {
    _s_174,
    _s_175,
    _s_601,
    _s_176
};
enum _s_177 {
    _s_178,
    _s_179,
    _s_180,
    _s_181,
    _s_182,
    _s_183,
    _s_184,
    _s_185,
    _s_186,
    _s_187,
    _s_188
};
class _s_189 {
protected:
    _s_177 op;
    std::vector<_s_189> _s_190;
    std::string _s_191;
public:
    _s_189(std::vector<_s_189> &_s_192, _s_177 _s_193) : op(_s_193), _s_190(_s_192) {};
    _s_189(const std::string &_s_191) : op(_s_182), _s_191(_s_191) {};
    const std::string &_s_194() const {
        return _s_191;
    }
    const std::vector<_s_189> &_s_195() const {
        return _s_190;
    };
    _s_177 _s_196() const {
        return op;
    }
    std::string _s_26() {
        if (op==_s_182) return "Var("+_s_191+")";
        std::ostringstream o;
        if (op==_s_180) o << "NOT(";
        if (op==_s_179) o << "OR(";
        if (op==_s_178) o << "AND(";
        if (op==_s_181) o << "X(";
        if (op==_s_183) o << "F(";
        if (op==_s_184) o << "G(";
        if (op==_s_185) o << "U(";
        if (op==_s_186) o << "EQ(";
        if (op==_s_187) return "TRUE";
        if (op==_s_188) return "FALSE";
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) o << ", ";
            o << _s_190[i]._s_26();
        }
        o << ")";
        return o.str();
    }
    bool _s_197() const {
        if ((op==_s_181) || (op==_s_183) || (op==_s_184) || (op==_s_185)) return false;
        bool _s_96 = true;
        for (uint i=0; i<_s_190.size(); i++) {
            _s_96 &= _s_190[i]._s_197();
        }
        return _s_96;
    }
    bool _s_198(std::vector<std::string> const &list) {
        if (op==_s_182) {
            std::vector<std::string>::const_iterator it = std::find(list.begin(),list.end(),_s_191);
            if (it==list.end()) return false;
            return true;
        }
        for (uint i=0; i<_s_190.size(); i++) {
            if (_s_190[i]._s_198(list)) return true;
        }
        return false;
    }
};
class _s_199 {
private:
    _s_146 _s_200;
    std::string _s_201;
    std::string _s_602;
    void _s_202(std::string &_s_126, int &_s_203, const char *_s_204) const;
    _s_189 _s_205(xmlNodePtr node) const;
    _s_189 _s_206(xmlNodePtr node) const;
    bool _s_207;
    bool _s_208;
    bool _s_566;
    _s_173 _s_209;
public:
    _s_199(const std::string _s_210);
    std::string _s_211() const;
    void _s_212(std::vector<std::string> &_s_126) const;
    void _s_213(std::vector<std::string> &_s_126) const;
    void _s_387(std::string &_s_11) const;
    std::string _s_215() const {
        return _s_201;
    };
    void _s_216(std::vector<_s_189> &_s_126) const;
    void _s_217(std::vector<_s_189> &_s_126) const;
    bool _s_218() const {
        return _s_207;
    };
    void _s_219(bool _s_220) {
        _s_207 = _s_220;
    };
    _s_173 _s_221() const {
        return _s_209;
    };
    void _s_222(_s_173 _s_220) {
        _s_209 = _s_220;
    };
    void _s_223(bool value) {
        _s_208 = value;
    };
    bool _s_224() const {
        return _s_208;
    };
    void _s_565(bool b) {
        _s_566 = b;
    };
    bool _s_567() const {
        return _s_566;
    };
    void _s_599(std::string s) {
        _s_602 = s;
    };
    std::string _s_600() const {
        return _s_602;
    };
};
class _s_225;
class _s_226;
class _s_227 : public _s_84 {
private:
    const _s_199 &_s_228;
    BFBddManager mgr;
    std::vector<_s_27> vars;
    std::vector<std::string> _s_229;
    std::vector<uint> _s_230;
    std::vector<uint> _s_231;
    std::vector<uint> _s_232;
    std::vector<uint> _s_233;
    _s_226 *_s_234;
    _s_225 *_s_235;
public:
    _s_227(const _s_199 &_s_228);
    ~_s_227();
    const _s_199 &_s_236() {
        return _s_228;
    }
    const std::vector< _s_27 > &_s_237() const {
        return vars;
    }
    const std::vector< std :: string > &_s_238() const {
        return _s_229;
    }
    const std::vector< uint > &_s_239() const {
        return _s_230;
    }
    const std::vector< uint > &_s_240() const {
        return _s_231;
    }
    const std::vector< uint > &_s_241() const {
        return _s_232;
    }
    const std::vector< uint > &_s_242() const {
        return _s_233;
    }
    BFBddManager &_s_245() {
        return mgr;
    }
    void _s_243();
    void _s_244();
    uint _s_246(const std::string &_s_191);
    uint _s_247(const std::string &_s_191);
    uint _s_248(const std::string &_s_191);
    void _s_85(std::vector<std::string> &_s_92) const;
    void _s_86(std::string type, std::vector<uint> &_s_93) const;
    _s_27 _s_87(uint _s_94) const {
        return vars[_s_94];
    };
    std::string _s_88(uint _s_94) const {
        return _s_229[_s_94];
    };
};
class _s_249 {
private:
    const _s_199 &_s_228;
    _s_10 _s_250;
    std::string _s_251;
    std::vector<int> _s_252;
    void _s_253();
    void _s_254();
    void _s_255();
public:
    _s_249(const _s_199 &_s_256);
    void _s_243();
};
class _s_227;
class _s_226;
class _s_225 : public _s_84 {
private:
    BFBddManager &mgr;
    _s_227 &_s_257;
    std::vector<_s_27> vars;
    std::vector<std::string> _s_229;
    std::vector<uint> _s_258;
    std::vector<uint> _s_259;
    const std::vector<uint> _s_260;
    const std::vector<uint> _s_261;
    std::vector<uint> _s_262;
    std::vector<uint> _s_263;
    _s_27 _s_264;
    _s_226 &_s_234;
    _s_28 _s_265;
    _s_28 _s_266;
    _s_28 _s_267;
    _s_29 *_s_268;
    _s_29 *_s_269;
    void _s_270();
    _s_28 _s_36(const std::vector<uint> _s_225::*list[]);
    void _s_271(_s_27 bdd, _s_28 _s_467) {
        _s_27 _s_59 = bdd._s_71(_s_467);
        assert((_s_59==mgr._s_33()) || (_s_59==mgr._s_34()));
    }
    _s_29 _s_272 ( const std::vector<uint> &_s_52) const;
public:
    _s_225(BFBddManager &_s_53, _s_227 &_s_273, const std::vector<_s_27> _s_274, const std::vector<std::string> _s_275, const std::vector<uint> _s_276, const std::vector<uint> _s_277, const std::vector<uint> _s_278, const std::vector<uint> _s_279, _s_226 &_s_280);
    ~_s_225();
    void _s_243();
    void _s_85(std::vector<std::string> &_s_92) const;
    void _s_86(std::string type, std::vector<uint> &_s_93) const;
    _s_27 _s_87(uint _s_94) const {
        return vars[_s_94];
    };
    std::string _s_88(uint _s_94) const {
        return _s_229[_s_94];
    };
    uint _s_247(const std::string &_s_191);
    uint _s_248(const std::string &_s_191);
    BFBddManager &_s_282() {
        return mgr;
    };
    _s_227 &_s_281() {
        return _s_257;
    };
};
class _s_226 : public _s_97 {
private:
    _s_227 &_s_283;
    _s_225 *_s_284;
    _s_27 *_s_285;
    _s_27 *_s_286;
    _s_27 *_s_287;
    _s_27 *_s_288;
    _s_27 _s_289;
    std::vector<_s_189> _s_290;
    std::vector<_s_189> _s_291;
    std::string _s_292(_s_189 root);
    std::vector<_s_101> _s_293;
    std::vector<std::vector<bool> > _s_294;
    std::vector<_s_101> _s_295;
    std::set<const _s_101*> _s_296;
    std::vector < std::vector< std::vector< uint > > > _s_297;
    std::vector < std::vector< std::vector< uint > > > _s_298;
    std::vector < std::vector< uint > > _s_299;
    std::vector < std::vector< uint > > _s_300;
    uint _s_301;
    _s_27 _s_302(const std::vector<uint> &first, const std::vector<uint> &second) const;
    _s_27 _s_303(const std::vector<uint> &first, const std::vector<uint> &second) const;
    _s_27 _s_304(const std::vector<uint> &first) const;
    void _s_305(std::string _s_306, _s_101 &_s_144);
    _s_27 _s_307(const _s_101 &_s_144, uint _s_52, uint to, _s_27 &_s_308) const;
    _s_28 _s_36(const std::vector<uint> &vars) const;
public:
    _s_226(_s_227 &_s_283);
    ~_s_226();
    void _s_309(_s_225 *_s_310);
    void _s_311(uint _s_312);
    void _s_313(std::vector<_s_27> &_s_314);
    _s_27 _s_315() const;
    _s_27 _s_316() const;
    _s_27 _s_317();
    _s_27 _s_318(_s_27 _s_319) const;
    _s_227 &_s_281() {
        return _s_283;
    };
    virtual BFBddManager &_s_98() {
        return _s_283._s_245();
    };
    virtual _s_27 _s_99(std::string name) const;
    virtual void _s_100(std::vector<std::string> &vars) {
        for (uint i=0; i<_s_283._s_237().size(); i++) {
            std::ostringstream s;
            s << "v" << i;
            vars.push_back(s.str());
        }
        vars.push_back("safe");
    };
};
void _s_320(const _s_189 *n, _s_225 &_s_284);
void _s_605(const _s_189 *n, _s_225 &_s_284, std::ofstream &_s_8);
class _s_321 {
private:
    _s_225 &_s_284;
    std::vector<uint> _s_322;
    std::vector<uint> _s_323;
    std::vector<uint> _s_230;
    std::vector<uint> _s_231;
    _s_27 _s_324;
public:
    _s_321(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329);
    void _s_243(_s_27 _s_330, const _s_27 &_s_331);
};
class _s_332 {
private:
    _s_225 &_s_284;
    std::vector<uint> _s_322;
    std::vector<uint> _s_323;
    std::vector<uint> _s_230;
    std::vector<uint> _s_231;
    _s_27 _s_324;
public:
    _s_332(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329);
    void _s_243(_s_27 _s_330, const _s_27 &_s_331);
};
class _s_598 {
private:
    _s_225 &_s_284;
    std::vector<uint> _s_322;
    std::vector<uint> _s_323;
    std::vector<uint> _s_230;
    std::vector<uint> _s_231;
    _s_27 _s_324;
    std::string _s_604;
public:
    _s_598(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329, std::string _s_603);
    void _s_243(_s_27 _s_330, const _s_27 &_s_331);
};
class _s_333 {
private:
    uint _s_334;
    std::string _s_191;
public:
    _s_333(uint _s_335, const std::string &_s_336) : _s_334(_s_335), _s_191(_s_336) {};
    uint _s_337() const {
        return _s_334;
    }
    const std::string &_s_194() const {
        return _s_191;
    }
    friend int operator<(const _s_333& _s_338, const _s_333& _s_339);
    friend int operator==(const _s_333& _s_338, const _s_333& _s_339);
};
class _s_226;
class _s_340 {
private:
    _s_225 &_s_284;
    BFBddManager &mgr;
    std::vector<_s_333> _s_341;
    std::vector<_s_189> _s_342;
    std::vector<uint> _s_343;
    std::vector<uint> _s_344;
    std::map<std::string, uint> _s_345;
    std::map<std::pair<uint,std::string>,std::pair<uint,uint> > _s_346;
    _s_27 _s_347;
    _s_27 _s_348;
    _s_27 _s_349;
    _s_27 _s_350;
    int _s_351(const _s_189 &n, int _s_352, std::set<_s_333> &_s_96);
    _s_27 _s_353(const _s_189 &n, int _s_352, int _s_354, std::string _s_355);
public:
    _s_340(_s_225 &_s_310);
    ~_s_340();
    void _s_243(bool _s_364);
    bool _s_357(_s_189 &_s_358);
    void _s_359(_s_189 &_s_358);
    _s_27 _s_360() {
        return _s_347;
    }
    _s_27 _s_361() {
        return _s_349;
    }
    _s_27 _s_362() {
        return _s_348;
    }
    _s_27 _s_316() {
        return _s_350;
    }
    bool _s_363() const {
        return _s_342.size()>0;
    }
};
class _s_575
{
private:
    _s_225 &_s_576;
    bool _s_577;
    static char _s_578();
    static _s_27 _s_579(_s_27 _s_580, std::vector<_s_27> &vars, bool _s_581);
    static _s_27 _s_582(BFBddManager &mgr, std::vector<_s_27> &vars);
    static void _s_583(_s_27 _s_580, std::vector<_s_27> &vars);
public:
    _s_575(_s_225 &_s_615, bool _p) : _s_576(_s_615), _s_577(_p) {};
    void _s_243(const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329, _s_27 _s_330, const _s_27 &_s_331);
};
class _s_365 {
private:
    static _s_365* _s_366;
    std::tr1::unordered_map<size_t, size_t> _s_367;
    _s_365() {
    }
public:
    ~_s_365() {
    }
    static _s_365* _s_368() {
        return _s_366;
    }
    static _s_365* _s_369() {
        if (_s_366 == 0) {
            _s_366 = new _s_365();
        }
        return _s_366;
    }
    static std::tr1::unordered_map<size_t,size_t>& _s_370() {
        return _s_369()->_s_367;
    }
    static void _s_374() {
        if (_s_366!=0) delete _s_366;
        _s_366 = 0;
    }
    static void _s_371(size_t _s_372) {
        if (_s_366!=0) _s_366->_s_367.erase(_s_372);
    }
};
_s_365* _s_365::_s_366 = 0;
class _s_373 {
public:
    ~_s_373() {
        _s_365::_s_374();
    }
};
namespace _s_375 {
_s_373 _s_376;
}
namespace std {
namespace tr1 {
template<> struct hash<_s_27> : public std::unary_function<_s_27, std::size_t> {
    std::size_t operator()(const _s_27 &b) const {
        return b._s_76();
    };
};
}
};
BFBddManager::BFBddManager(uint maxMemoryInMB, float reorderingMaxBlowup) : _s_30(0), _s_31(true) {
    mgr = Cudd_Init(0,0,CUDD_UNIQUE_SLOTS,CUDD_CACHE_SLOTS,(long)maxMemoryInMB*1024UL*1024UL);
    Cudd_AutodynEnable(mgr,CUDD_REORDER_SIFT);
    Cudd_SetMaxGrowth(mgr,reorderingMaxBlowup);
    Cudd_SetMinHit(mgr,1);
    _s_42(true);
    _s_365::_s_370()[(size_t)mgr] = (size_t)this;
}
BFBddManager::~BFBddManager() {
    _s_365::_s_371((size_t)mgr);
    int nofLeft = Cudd_CheckZeroRef(mgr);
    if (nofLeft != 0) {
        std::cerr << "Warning: " << nofLeft << " referenced nodes in the BDD manager left on destruction!\n";
    }
    Cudd_Quit(mgr);
}
BFBddManager *_s_27::_s_60() const {
    return (BFBddManager*)(_s_365::_s_370()[(size_t)mgr]);
}
void BFBddManager::_s_44() {
    cuddGarbageCollect(mgr,1);
}
void BFBddManager::_s_43() {
    Cudd_PrintInfo(mgr,stdout);
}
_s_27::_s_77::_s_77(_s_27 &bdd) {
    CUDD_VALUE_TYPE value;
    gen = Cudd_FirstCube(bdd.mgr,bdd.node,&cube,&value);
    mgr = bdd._s_60();
    mgr->_s_30++;
    mgr->_s_42(false);
}
_s_27::_s_77::~_s_77() {
    if (gen!=NULL) Cudd_GenFree(gen);
    mgr->_s_30--;
    if (mgr->_s_30==0) mgr->_s_32(mgr->_s_31);
}
void BFBddManager::_s_41(const std::vector<_s_27> &_s_612) {
    std::set<DdHalfWord> _s_618;
    DdHalfWord min = std::numeric_limits<DdHalfWord>::max();
    DdHalfWord max = std::numeric_limits<DdHalfWord>::min();
    for (uint i=0; i<_s_612.size(); i++) {
        DdNode *node = _s_612[i].node;
        DdHalfWord index = ((Cudd_Regular(node))->index);
        if (index<min) min = index;
        if (index>max) max = index;
        _s_618.insert(index);
    }
    if ((uint)(max-min+1)!=_s_618.size()) throw "Error in BFBddManager::groupVariables(const std::vector<BFBdd> &which) - Can only group continuous variables!\n";
    Cudd_MakeTreeNode(mgr,min,max-min+1,MTR_DEFAULT);
}
void BFBddManager::_s_45() {
    if (Cudd_ReduceHeap(mgr, CUDD_REORDER_EXACT, 1)==0) {
        std::cerr << "Warning! Exact Reordering failed: ";
        switch (Cudd_ReadErrorCode (mgr)) {
        case CUDD_NO_ERROR:
            std::cerr << "CUDD_NO_ERROR\n";
            break;
        case CUDD_MEMORY_OUT:
            std::cerr << "CUDD_MEMORY_OUT\n";
            break;
        case CUDD_TOO_MANY_NODES:
            std::cerr << "CUDD_TOO_MANY_NODES\n";
            break;
        case CUDD_MAX_MEM_EXCEEDED:
            std::cerr << "CUDD_MAX_MEM_EXCEEDED\n";
            break;
        case CUDD_INVALID_ARG:
            std::cerr << "CUDD_INVALID_ARG\n";
            break;
        case CUDD_INTERNAL_ERROR:
            std::cerr << "CUDD_INTERNAL_ERROR\n";
            break;
        default:
            std::cerr << "Unknown\n";
            break;
        }
    }
};
void BFBddManager::_s_46() {
    if (Cudd_ReduceHeap(mgr, CUDD_REORDER_SIFT, 10)==0) {
        std::cerr << "Warning! Intermediate optimizing reordering failed: ";
        switch (Cudd_ReadErrorCode (mgr)) {
        case CUDD_NO_ERROR:
            std::cerr << "CUDD_NO_ERROR\n";
            break;
        case CUDD_MEMORY_OUT:
            std::cerr << "CUDD_MEMORY_OUT\n";
            break;
        case CUDD_TOO_MANY_NODES:
            std::cerr << "CUDD_TOO_MANY_NODES\n";
            break;
        case CUDD_MAX_MEM_EXCEEDED:
            std::cerr << "CUDD_MAX_MEM_EXCEEDED\n";
            break;
        case CUDD_INVALID_ARG:
            std::cerr << "CUDD_INVALID_ARG\n";
            break;
        case CUDD_INTERNAL_ERROR:
            std::cerr << "CUDD_INTERNAL_ERROR\n";
            break;
        default:
            std::cerr << "Unknown\n";
            break;
        }
    }
};
bool BFBddManager::_s_47(std::string _s_48, std::vector<_s_27> &_s_49) {
    DdNode** list = new DdNode*[_s_49.size()];
    for (uint i=0; i<_s_49.size(); i++) list[i] = _s_49[i].node;
    int value =
        Dddmp_cuddBddArrayStore(
            mgr,
            NULL,
            _s_49.size(),
            list,
            NULL,
            NULL,
            NULL,
            DDDMP_MODE_BINARY,
            Dddmp_VarInfoType(),
            (char*)(_s_48.c_str()),
            NULL
        );
    if (value==DDDMP_FAILURE) {
        std::cerr << "Warning: BFBDDManager::writeToFile(std::string filename, std::vector<BF> &bdds) failed!\n";
        return true;
    }
    delete[] list;
    return false;
}
bool BFBddManager::_s_47(FILE *file, std::vector<_s_27> &_s_49) {
    DdNode** list = new DdNode*[_s_49.size()];
    for (uint i=0; i<_s_49.size(); i++) list[i] = _s_49[i].node;
    int value =
        Dddmp_cuddBddArrayStore(
            mgr,
            NULL,
            _s_49.size(),
            list,
            NULL,
            NULL,
            NULL,
            DDDMP_MODE_BINARY,
            Dddmp_VarInfoType(),
            NULL,
            file
        );
    if (value==DDDMP_FAILURE) {
        std::cerr << "Warning: BFBDDManager::writeToFile(std::string filename, std::vector<BF> &bdds) failed!\n";
        return true;
    }
    delete[] list;
    return false;
}
bool BFBddManager::_s_50(std::string _s_48, std::vector<_s_27> &_s_49) {
    DdNode **_s_126 = NULL;
    int value = Dddmp_cuddBddArrayLoad(
                    mgr,
                    DDDMP_ROOT_MATCHLIST,
                    NULL,
                    DDDMP_VAR_MATCHIDS ,
                    NULL,
                    NULL,
                    NULL,
                    DDDMP_MODE_BINARY,
                    (char*)(_s_48.c_str()),
                    NULL,
                    &_s_126
                );
    if (value==DDDMP_FAILURE) {
        std::cerr << "Warning: BFBDDManager::readFromTile(std::string filename, int howMany, std::vector<BFBdd> &bdds) failed!\n";
        return true;
    }
    for (int i=0; i<value; i++) {
        _s_49.push_back(_s_27(mgr,_s_126[i]));
        Cudd_RecursiveDeref(mgr,_s_126[i]);
    }
    free(_s_126);
    return false;
}
bool BFBddManager::_s_50(FILE *file, std::vector<_s_27> &_s_49) {
    DdNode **_s_126 = NULL;
    int value = Dddmp_cuddBddArrayLoad(
                    mgr,
                    DDDMP_ROOT_MATCHLIST,
                    NULL,
                    DDDMP_VAR_MATCHIDS ,
                    NULL,
                    NULL,
                    NULL,
                    DDDMP_MODE_BINARY,
                    NULL,
                    file,
                    &_s_126
                );
    if (value==DDDMP_FAILURE) {
        std::cerr << "Warning: BFBDDManager::readFromTile(std::string filename, int howMany, std::vector<BFBdd> &bdds) failed!\n";
        return true;
    }
    for (int i=0; i<value; i++) {
        _s_49.push_back(_s_27(mgr,_s_126[i]));
        Cudd_RecursiveDeref(mgr,_s_126[i]);
    }
    free(_s_126);
    return false;
}
double _s_377(DdManager *dd, DdNode *_s_379, DdNode *cube, std::map<DdNode*,double> &_s_378) {
    DdNode *cubeNext;
    if (Cudd_Regular(cube)==cube) {
        cubeNext = cuddT(cube);
    } else {
        if (!Cudd_IsConstant(cube)) {
            cube = Cudd_Regular(cube);
            cubeNext = (DdNode*)(((size_t)(cuddE(cube)) ^ 0x1));
        } else {
            return (_s_379==dd->one)?1:0;
        }
    }
    if (_s_378.count(_s_379)>0) return _s_378[_s_379];
    if (Cudd_IsConstant(_s_379)) {
        if (Cudd_IsConstant(cube)) {
            return (_s_379==dd->one)?1:0;
        } else {
            return 2*_s_377(dd,_s_379,cubeNext,_s_378);
        }
    }
    size_t _s_380 = (Cudd_Regular(_s_379)==_s_379?0:1);
    DdNode *_s_580 = Cudd_Regular(_s_379);
    if (Cudd_IsConstant(cube)) return std::numeric_limits<double>::quiet_NaN();
    int i1 = cuddI(dd,cube->index);
    int i2 = cuddI(dd,_s_580->index);
    if (i1<i2) {
        double value = 2*_s_377(dd,(DdNode*)((size_t)_s_580 ^ _s_380),cubeNext,_s_378);
        _s_378[_s_379] = value;
        return value;
    } else if (i1>i2) {
        return std::numeric_limits<double>::quiet_NaN();
    } else {
        double value = _s_377(dd,(DdNode*)((size_t)(cuddT(_s_580)) ^ _s_380),cubeNext,_s_378)
                       + _s_377(dd,(DdNode*)((size_t)(cuddE(_s_580)) ^ _s_380),cubeNext,_s_378);
        _s_378[_s_379] = value;
        return value;
    }
}
void BFBddManager::_s_32(bool _s_611) {
    if (_s_611)
        Cudd_AutodynEnable(mgr,CUDD_REORDER_SAME);
    else
        Cudd_AutodynDisable(mgr);
}
void BFBddManager::_s_42(bool _s_611) {
    _s_31 = _s_611;
    if (_s_30==0) _s_32(_s_611);
}
void _s_27::_s_75(const _s_28& cube, std::list<std::pair<DdHalfWord,bool> > &_s_381) const {
    DdNode *_s_107 = node;
    DdNode *_s_142 = cube.cube;
    bool _s_526 = false;
    while ((Cudd_Regular(_s_107)->index!=CUDD_CONST_INDEX) || (Cudd_Regular(_s_142)->index!=CUDD_CONST_INDEX)) {
        assert(((size_t)_s_142 & 1)==0);
        assert((Cudd_Regular(_s_107)->index==CUDD_CONST_INDEX) || (Cudd_Regular(_s_142)->index==CUDD_CONST_INDEX) || (cuddI(mgr,Cudd_Regular(_s_107)->index) <= cuddI(mgr,_s_142->index)));
        if ((Cudd_Regular(_s_107)->index==CUDD_CONST_INDEX) || (cuddI(mgr,Cudd_Regular(_s_107)->index) > cuddI(mgr,_s_142->index))) {
            _s_381.push_back(std::pair<DdHalfWord,bool>(_s_142->index,false));
            _s_142 = cuddT(_s_142);
        } else {
            _s_526 ^= ((size_t)_s_107 & 1)>0;
            if (_s_526) {
                if (cuddE(Cudd_Regular(_s_107)) == DD_ONE(mgr)) {
                    _s_381.push_back(std::pair<DdHalfWord,bool>(_s_142->index,true));
                    _s_107 = cuddT(Cudd_Regular(_s_107));
                } else {
                    _s_381.push_back(std::pair<DdHalfWord,bool>(_s_142->index,false));
                    _s_107 = cuddE(Cudd_Regular(_s_107));
                }
            } else {
                if (cuddE(Cudd_Regular(_s_107)) == Cudd_Not(DD_ONE(mgr))) {
                    _s_381.push_back(std::pair<DdHalfWord,bool>(_s_142->index,true));
                    _s_107 = cuddT(Cudd_Regular(_s_107));
                } else {
                    _s_381.push_back(std::pair<DdHalfWord,bool>(_s_142->index,false));
                    _s_107 = cuddE(Cudd_Regular(_s_107));
                }
            }
            _s_142 = cuddT(_s_142);
        }
    }
}
_s_28 BFBddManager::_s_36(const _s_27 *vars, const int * _s_37, int n) {
    DdNode **vars2 = new DdNode*[n];
    for (int i=0; i<n; i++) vars2[i] = vars[i].node;
    DdNode *cube = Cudd_bddComputeCube(mgr,vars2,const_cast<int*>(_s_37),n);
    _s_28 _s_382(mgr,cube,n);
    delete[] vars2;
    return _s_382;
}
_s_28 BFBddManager::_s_36(const std::vector<_s_27> &vars) {
    DdNode **vars2 = new DdNode*[vars.size()];
    int *_s_37 = new int[vars.size()];
    for (unsigned int i=0; i<vars.size(); i++) {
        vars2[i] = vars[i].node;
        _s_37[i] = 1;
    }
    DdNode *cube = Cudd_bddComputeCube(mgr,vars2,_s_37,vars.size());
    _s_28 _s_382(mgr,cube,vars.size());
    delete[] vars2;
    delete[] _s_37;
    return _s_382;
}
_s_29 BFBddManager::_s_38(const std::vector<_s_27> &_s_52) {
    _s_29 v;
    v._s_83 = new DdNode*[_s_52.size() ];
    for ( unsigned int i=0; i<_s_52.size(); i++ ) {
        v._s_83[i] = ( _s_52 ) [i].node;
        Cudd_Ref(v._s_83[i]);
    }
    v.nofNodes = _s_52.size();
    v.mgr = mgr;
    return v;
}
_s_27 _s_27::_s_75(const _s_28& cube) const {
    std::list<std::pair<DdHalfWord,bool> > _s_381;
    _s_75(cube,_s_381);
    DdNode *_s_383 = DD_ONE(mgr);
    Cudd_Ref(_s_383);
    for (std::list<std::pair<DdHalfWord,bool> >::iterator it = _s_381.begin(); it!=_s_381.end(); it++) {
        DdNode *var = Cudd_bddIthVar(mgr, it->first);
        if (!(it->second)) var = Cudd_Not(var);
        DdNode *_s_384 = Cudd_bddAnd(mgr,var,_s_383);
        Cudd_Ref(_s_384);
        Cudd_RecursiveDeref(mgr,_s_383);
        _s_383 = _s_384;
    }
    Cudd_Deref(_s_383);
    return _s_27(mgr,_s_383);
}
double _s_27::_s_58(const _s_28 &_cube) const {
    DdNode *cube = _cube.cube;
    std::map<DdNode*,double> _s_378;
    return _s_377(mgr,node,cube,_s_378);
}
_s_249::_s_249(const _s_199 &_s_256) : _s_228(_s_256) {
}
void _s_249::_s_255() {
    std::cout << "\n==============================[Synthesis]=====================================\n\n";
    _s_227 _s_283(_s_228);
    _s_283._s_243();
}
void _s_249::_s_243() {
    _s_255();
}
using namespace std;
int main(int argv, char **args) {
    cout << "Unbeast v.0.6b -- (C) 2009-2011 by Ruediger Ehlers" << endl;
    try {
        std::string _s_201 = "";
        std::string _s_602 = "";
        bool _s_385 = false;
        bool _s_208 = false;
        bool _s_566 = false;
        _s_173 _s_209 = _s_176;
        for (int i=1; i<argv; i++) {
            if (args[i][0] == '-') {
                std::string param = args[i];
                if (param=="--unsat") {
                    _s_385 = true;
                } else if (param=="--synDNF") {
                    _s_209 = _s_175;
                } else if (param=="--synExport") {
                    _s_209 = _s_601;
                    if (i<argv-1) {
                        _s_602 = args[++i];
                    } else {
                        std::cerr << "Error: If \"--synExport\" options is used, a file name for the output file must be provided right next to that particular option.\n";
                        return 2;
                    }
                } else if (param=="--synBDD") {
                    _s_209 = _s_174;
                } else if (param=="--noCounterOptimizationInLivenessPart") {
                    _s_208 = true;
                } else if (param=="--runSimulator") {
                    _s_566 = true;
                } else {
                    std::cerr << "Invalid option: " << param << "\n";
                    return 2;
                }
            } else {
                if (_s_201!="") {
                    std::cerr << "More than one input file name given: " << args[i] << "\n";
                    return 2;
                }
                _s_201 = args[i];
                std::cerr << "-> Using input file " << _s_201 << endl;
            }
        }
        if (_s_201=="") std::cerr << "No input file given. Using piped input.\n";
        _s_199 _s_228(_s_201);
        _s_228._s_219(_s_385);
        _s_228._s_222(_s_209);
        _s_228._s_223(_s_208);
        _s_228._s_565(_s_566);
        _s_228._s_599(_s_602);
        cout << "Settings have been read. Details:" << endl;
        cout << _s_228._s_211() << endl;
        _s_249 _s_619(_s_228);
        _s_619._s_243();
    } catch (_s_2 e) {
        cout << "Caught exception: " << e._s_19() << endl;
        return 1;
    } catch (const char *c) {
        cout << "Exception: " << c << "\n";
    }
    return 0;
}
std::string _s_386(std::string in) {
    if (in.substr(0,3)=="in_") throw _s_2("Variable names must not start with \"in_\"");
    if (in.substr(0,4)=="out_") throw _s_2("Variable names must not start with \"out_\"");
    if (in.substr(0,6)=="Safety_") throw _s_2("Variable names must not start with \"Safety_\"");
    if (in.substr(0,3)=="Go_") throw _s_2("Variable names must not start with \"Go_\"");
    if (in.substr(0,4)=="LTL_") throw _s_2("Variable names must not start with \"LTL_\"");
    size_t found;
    found=in.find('.');
    if (found!=std::string::npos)
        throw _s_2("Variable names must not contain a dot");
    return in;
}
_s_199::_s_199(const std::string _s_210) : _s_200(_s_210), _s_201(_s_210), _s_207(false), _s_209(_s_176) {
}
void _s_199::_s_212(std::vector<std::string> &_s_126) const {
    _s_146::_s_149 _s_59;
    _s_200._s_167("//GlobalInputs",_s_59);
    if (_s_59.empty()) {
        throw _s_2("No globalInputs found.");
    } else if (_s_59.size()>1) {
        throw _s_2("More than one GlobalInputs block found.");
    } else {
        _s_146::_s_149 inputs;
        _s_200._s_167("//GlobalInputs/Bit",inputs);
        if (inputs.empty())
            throw _s_2("The list of global inputs is empty.");
        for (uint i=0; i<inputs.size(); i++) {
            _s_126.push_back(_s_386(_s_200._s_170(inputs[i])));
        }
    }
}
void _s_199::_s_202(std::string &_s_126, int &line, const char *_s_204) const {
    _s_146::_s_149 _s_59;
    std::string _s_620("//");
    _s_620 = _s_620 + _s_204;
    _s_200._s_167(_s_620.c_str(),_s_59);
    if (_s_59.empty()) {
        throw _s_2(std::string("Required information not found in the input XML file: ")+_s_204+")");
    } else if (_s_59.size()>1) {
        throw _s_2(std::string("Shouldn't happen (contrary to DTD: 2, Element ")+_s_204+")");
    } else {
        _s_126 = _s_200._s_170(_s_59[0]);
        line = _s_59[0]->line;
    }
}
void _s_199::_s_387(std::string &_s_11) const {
    int line;
    _s_202(_s_11,line,"PathToLTLCompiler");
}
_s_189 _s_199::_s_205(xmlNodePtr node) const {
    if (xmlStrEqual(node->name,BAD_CAST "Not")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular NOT node in safety specification");
                _s_190.push_back(_s_205(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty safety specification in Not node",node->line);
        return _s_189(_s_190,_s_180);
    }
    if (xmlStrEqual(node->name,BAD_CAST "X")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular X node in safety specification");
                _s_190.push_back(_s_205(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty safety specification in X node",node->line);
        return _s_189(_s_190,_s_181);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Or")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_205(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty safety specification in Or node",node->line);
        return _s_189(_s_190,_s_179);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Iff")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_205(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty safety specification in Iff node",node->line);
        return _s_189(_s_190,_s_186);
    }
    if (xmlStrEqual(node->name,BAD_CAST "And")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_205(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty safety specification in And node",node->line);
        return _s_189(_s_190,_s_178);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Var")) {
        std::string name = _s_386(_s_200._s_170(node));
        std::vector<std::string> vars;
        _s_212(vars);
        _s_213(vars);
        std::vector<std::string>::iterator it = std::find(vars.begin(),vars.end(),name);
        if (it==vars.end()) {
            std::ostringstream out;
            out << "Unknown variable appeared in safety specification: " << name << "(XML line: " << node->line << ") - Are you using white box outputs?";
            throw new _s_2(out.str());
        }
        return _s_189(name);
    }
    throw _s_4("Illegal/Unknown node type in safety specification: ",node->line);
}
_s_189 _s_199::_s_206(xmlNodePtr node) const {
    if (xmlStrEqual(node->name,BAD_CAST "True")) {
        std::vector<_s_189> _s_190;
        if (node->children!=NULL) throw _s_2("Non-empty True node in specification");
        return _s_189(_s_190,_s_187);
    }
    if (xmlStrEqual(node->name,BAD_CAST "False")) {
        std::vector<_s_189> _s_190;
        if (node->children!=NULL) throw _s_2("Non-empty False node in specification");
        return _s_189(_s_190,_s_188);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Not")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular NOT node in safety specification");
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in Not node",node->line);
        return _s_189(_s_190,_s_180);
    }
    if (xmlStrEqual(node->name,BAD_CAST "X")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular X node in LTL specification");
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in X node",node->line);
        return _s_189(_s_190,_s_181);
    }
    if (xmlStrEqual(node->name,BAD_CAST "G")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular G node in LTL specification");
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in G node",node->line);
        return _s_189(_s_190,_s_184);
    }
    if (xmlStrEqual(node->name,BAD_CAST "F")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                if (found) throw _s_2("Non-singular F node in LTL specification");
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in F node",node->line);
        return _s_189(_s_190,_s_183);
    }
    if (xmlStrEqual(node->name,BAD_CAST "U")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (_s_190.size()!=2)
            _s_4("LTL specification contains Until node with more or less than two subnodes",node->line);
        if (!found) throw _s_4("Empty LTL specification in Until node",node->line);
        return _s_189(_s_190,_s_185);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Iff")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (_s_190.size()!=2)
            _s_4("LTL specification contains Iff node with more or less than two subnodes",node->line);
        if (!found) throw _s_4("Empty LTL specification in Iff node",node->line);
        return _s_189(_s_190,_s_186);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Or")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in Or node",node->line);
        return _s_189(_s_190,_s_179);
    }
    if (xmlStrEqual(node->name,BAD_CAST "And")) {
        std::vector<_s_189> _s_190;
        bool found = false;
        for (xmlNodePtr _s_464 = node->children; _s_464!=NULL; _s_464 = _s_464->next) {
            if (_s_464->type == XML_ELEMENT_NODE) {
                _s_190.push_back(_s_206(_s_464));
                found = true;
            }
        }
        if (!found) throw _s_4("Empty LTL specification in And node",node->line);
        return _s_189(_s_190,_s_178);
    }
    if (xmlStrEqual(node->name,BAD_CAST "Var")) {
        std::string name = _s_386(_s_200._s_170(node));
        std::vector<std::string> vars;
        _s_212(vars);
        _s_213(vars);
        std::vector<std::string>::iterator it = std::find(vars.begin(),vars.end(),name);
        if (it==vars.end())
            throw _s_4("Unknown variable appeared in safety specification: "+name,node->line);
        return _s_189(name);
    }
    std::ostringstream _s_388;
    _s_388 << "Illegal/Unknown node type in LTL specification: " << node->name;
    throw _s_4(_s_388.str(),node->line);
}
void _s_199::_s_216(std::vector<_s_189> &_s_126) const {
    _s_146::_s_149 _s_59;
    _s_200._s_167("//Specification",_s_59);
    if (_s_59.empty()) {
        throw _s_2("Settings::getSpecificationNodes called although there is no specification");
    } else if (_s_59.size()>1) {
        throw _s_2("More than one specification found.");
    }
    for (xmlNodePtr child = _s_59[0]->children; child!=NULL; child = child->next) {
        if (child->type == XML_ELEMENT_NODE) {
            if (xmlStrEqual(child->name,BAD_CAST "LTL")) {
                bool found = false;
                for (xmlNodePtr _s_464 = child->children; _s_464!=NULL; _s_464 = _s_464->next) {
                    if (_s_464->type == XML_ELEMENT_NODE) {
                        if (found) throw _s_2("Non-singular LTL specification");
                        _s_126.push_back(_s_206(_s_464));
                        found = true;
                    }
                }
                if (!found) std::cout << "Warning: No LTL specification" << std::endl;
            } else if (xmlStrEqual(child->name,BAD_CAST "Safety")) {
            } else {
                throw _s_2("Unknown subnode type of the Specification node");
            }
        }
    }
}
void _s_199::_s_217(std::vector<_s_189> &_s_126) const {
    _s_146::_s_149 _s_59;
    _s_200._s_167("//Assumptions",_s_59);
    if (_s_59.empty()) {
        return;
    } else if (_s_59.size()>1) {
        throw _s_2("More than one assumption node found.");
    }
    for (xmlNodePtr child = _s_59[0]->children; child!=NULL; child = child->next) {
        if (child->type == XML_ELEMENT_NODE) {
            if (xmlStrEqual(child->name,BAD_CAST "LTL")) {
                bool found = false;
                for (xmlNodePtr _s_464 = child->children; _s_464!=NULL; _s_464 = _s_464->next) {
                    if (_s_464->type == XML_ELEMENT_NODE) {
                        if (found) throw _s_2("Non-singular LTL specification");
                        _s_126.push_back(_s_206(_s_464));
                        found = true;
                    }
                }
                if (!found) std::cout << "Warning: No LTL specification" << std::endl;
            } else if (xmlStrEqual(child->name,BAD_CAST "Liveness")) {
            } else if (xmlStrEqual(child->name,BAD_CAST "NegatedLiveness")) {
            } else {
                throw _s_2("Unknown subnode type of the Specification node");
            }
        }
    }
}
void _s_199::_s_213(std::vector<std::string> &_s_126) const {
    _s_146::_s_149 _s_59;
    _s_200._s_167("//GlobalOutputs",_s_59);
    if (_s_59.empty()) {
        throw _s_2("No globalOutputs found.");
    } else if (_s_59.size()>1) {
        throw _s_2("More than one GlobalOutputs block found.");
    } else {
        _s_146::_s_149 inputs;
        _s_200._s_167("//GlobalOutputs/Bit",inputs);
        if (inputs.empty())
            throw _s_2("The list of global outputs is empty.");
        for (uint i=0; i<inputs.size(); i++) {
            _s_126.push_back(_s_386(_s_200._s_170(inputs[i])));
        }
    }
}
std::string _s_199::_s_211() const {
    std::ostringstream _s_621;
    {
        _s_621 << "Input variables ";
        std::vector<std::string> _s_622;
        _s_212(_s_622);
        _s_621 << "("<<_s_622.size()<<"): ";
        for (uint i=0; i<_s_622.size(); i++) {
            if (i != 0) _s_621 << ", ";
            _s_621 << _s_622[i];
        }
        _s_621 << std::endl;
    }
    {
        _s_621 << "Output variables ";
        std::vector<std::string> _s_622;
        _s_213(_s_622);
        _s_621 << "("<<_s_622.size()<<"): ";
        for (uint i=0; i<_s_622.size(); i++) {
            if (i != 0) _s_621 << ", ";
            _s_621 << _s_622[i];
        }
        _s_621 << std::endl;
    }
    return _s_621.str();
}
_s_146::_s_146()
{
    _s_147 = xmlNewDoc(BAD_CAST "1.0");
    _s_148 = xmlXPathNewContext(_s_147);
}
_s_146::_s_146(const std::string &_s_153)
{
    _s_148 = NULL;
    _s_147 = NULL;
    if (_s_153.empty()) {
        _s_147 = xmlReadFd(0, "stdin", NULL, XML_PARSE_DTDVALID);
    } else {
        _s_147 = xmlReadFile(_s_153.c_str(), NULL, XML_PARSE_DTDVALID);
    }
    if (_s_147==NULL) throw _s_2("Unable to read XML input stream");
    _s_148 = xmlXPathNewContext(_s_147);
}
_s_146::_s_146(const std::string &_s_154, const std::string &_s_153)
{
    _s_147 = xmlReadMemory(_s_154.c_str(), _s_154.length(), _s_153.c_str(), NULL, 0);
    _s_148 = xmlXPathNewContext(_s_147);
}
_s_146::~_s_146()
{
    if (_s_148!=NULL) xmlXPathFreeContext(_s_148);
    if (_s_147!=NULL) xmlFreeDoc(_s_147);
}
xmlNodePtr _s_146::_s_158(const std::string &name)
{
    xmlNodePtr root = xmlNewNode(NULL, BAD_CAST name.c_str());
    xmlDocSetRootElement(_s_147, root);
    return root;
}
xmlNodePtr _s_146::_s_159(xmlNodePtr _s_161, const std::string &name)
{
    return xmlNewChild(_s_161, NULL, BAD_CAST name.c_str(), NULL);
}
xmlNodePtr _s_146::_s_159(xmlNodePtr _s_161, const std::string &name, const
                          std::string &value)
{
    return xmlNewChild(_s_161, NULL, BAD_CAST name.c_str(), BAD_CAST value.c_str());
}
void _s_146::_s_160(xmlNodePtr node, const std::string &value)
{
    xmlNodeSetContent(node, BAD_CAST value.c_str());
}
xmlNodePtr _s_146::_s_160(const std::string &_s_152, const std::string &value)
{
    xmlNodePtr n = _s_164(_s_152);
    if (n) {
        _s_160(n, value);
    }
    return n;
}
void _s_146::_s_162(xmlNodePtr node, const std::string &name, const
                    std::string &value)
{
    xmlSetProp(node, BAD_CAST name.c_str(), BAD_CAST value.c_str());
}
void _s_146::_s_155(const std::string &_s_153) const
{
    xmlSaveFile(_s_153.c_str(), _s_147);
}
void _s_146::_s_156(std::string &_s_157) const
{
    xmlChar *_s_389;
    int _s_390;
    xmlDocDumpFormatMemory(_s_147, &_s_389, &_s_390, 1);
    _s_157 += (char *) _s_389;
    xmlFree(_s_389);
}
void _s_146::_s_163(xmlNodePtr n)
{
    _s_148->node = n;
}
xmlNodePtr _s_146::_s_164(const std::string &_s_152) const
{
    xmlXPathObjectPtr xpathObj = xmlXPathEvalExpression(BAD_CAST _s_152.c_str(), _s_148);
    if (!xpathObj) return NULL;
    xmlNodePtr n = NULL;
    if (xpathObj->nodesetval->nodeNr) {
        n = xpathObj->nodesetval->nodeTab[0];
    }
    xmlXPathFreeObject(xpathObj);
    return n;
}
xmlNodePtr _s_146::_s_164(xmlNodePtr _s_165, const std::string &_s_152) const
{
    xmlNodePtr tmp = _s_148->node;
    _s_148->node = _s_165;
    xmlNodePtr n = _s_164(_s_152);
    _s_148->node = tmp;
    return n;
}
bool _s_146::_s_166(const std::string &_s_152) const
{
    return (_s_164(_s_152) != NULL);
}
void _s_146::_s_167(const std::string &_s_152, _s_149 &_s_59) const
{
    _s_59._s_167(_s_152, _s_148);
}
void _s_146::_s_168(const std::string &_s_152, _s_149 &_s_59, const xmlNodePtr _s_165) const
{
    xmlNodePtr tmp = _s_148->node;
    _s_148->node = _s_165;
    _s_59._s_167(_s_152, _s_148);
    _s_148->node = tmp;
}
std::string _s_146::_s_169(const std::string &_s_152) const
{
    xmlNodePtr n = _s_164(_s_152);
    if (n) {
        return _s_170(n);
    }
    return std::string();
}
std::string _s_146::_s_169(xmlNodePtr _s_165, const std::string &_s_152) const
{
    xmlNodePtr tmp = _s_148->node;
    _s_148->node = _s_165;
    std::string ret = _s_169(_s_152);
    _s_148->node = tmp;
    return ret;
}
std::string _s_146::_s_170(xmlNodePtr node)
{
    xmlChar *val = xmlNodeGetContent(node);
    std::string ret = (const char *) val;
    xmlFree(val);
    return ret;
}
std::string _s_146::_s_171(xmlNodePtr node)
{
    const xmlChar *val = node->name;
    std::string ret = (const char *) val;
    return ret;
}
std::string _s_146::_s_172(xmlNodePtr node, const std::string &name)
{
    xmlChar *val = xmlGetProp(node, BAD_CAST name.c_str());
    if (val) {
        std::string ret = (const char *) val;
        xmlFree(val);
        return ret;
    } else {
        return std::string();
    }
}
_s_146::_s_149::_s_149()
    : _s_150(NULL)
{
}
_s_146::_s_149::~_s_149()
{
    free();
}
void _s_146::_s_149::free()
{
    if (_s_150) {
        xmlXPathFreeObject(_s_150);
        _s_150 = NULL;
    }
}
void _s_146::_s_149::_s_167(const std::string &_s_152, xmlXPathContextPtr _s_151)
{
    free();
    _s_150 = xmlXPathEvalExpression(BAD_CAST _s_152.c_str(), _s_151);
}
xmlNodePtr _s_146::_s_149::operator[](size_t index) const
{
    return _s_150->nodesetval->nodeTab[index];
}
size_t _s_146::_s_149::size() const
{
    return _s_150->nodesetval->nodeNr;
}
bool _s_146::_s_149::empty() const
{
    return (size() == 0);
}
_s_321::_s_321(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329): _s_284(_s_310), _s_322(_s_325), _s_323(_s_326), _s_230(_s_327), _s_231(_s_328) {
    std::cout << "Extracting strategy: Computing combined transition relation...";
    std::cout.flush();
    _s_324 = _s_284._s_281()._s_245()._s_33();
    for (std::vector<_s_27>::const_iterator it = _s_329.begin(); it!=_s_329.end(); it++) {
        _s_324 &= *it;
    }
    std::cout << "done!" << std::endl;
}
namespace std {
namespace tr1 {
template<> struct hash<std::pair<bool,_s_27> > : public std::unary_function<std::pair<bool,_s_27> , std::size_t> {
    std::size_t operator()(const std::pair<bool,_s_27> &b) const {
        return b.second._s_76() + b.first?0:0x1378393;
    };
};
}
};
class _s_391 {
public:
    std::tr1::unordered_set<std::pair<bool,_s_27> > _s_392;
};
typedef enum { _s_393, _s_394, _s_395, _s_396 } _s_397;
void _s_321::_s_243(_s_27 _s_330, const _s_27 &_s_331) {
    BFBddManager &mgr = _s_284._s_281()._s_245();
    std::tr1::unordered_map<int,_s_397> _s_398;
    std::tr1::unordered_map<int,_s_27> _s_399;
    std::tr1::unordered_map<int,std::string> _s_400;
    std::vector<_s_27> _s_401;
    std::vector<_s_27> _s_402;
    std::vector<_s_27> _s_403;
    std::vector<_s_27> _s_404;
    std::vector<_s_27> _s_405;
    std::vector<_s_27> _s_406;
    std::vector<_s_27> _s_407;
    for (std::vector<uint>::iterator it = _s_322.begin(); it!=_s_322.end(); it++) {
        _s_401.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_401.back());
        _s_398[_s_401.back()._s_68()] = _s_393;
        _s_399[_s_401.back()._s_68()] = _s_401.back();
        _s_400[_s_401.back()._s_68()] = _s_284._s_88(*it);
    }
    for (std::vector<uint>::iterator it = _s_323.begin(); it!=_s_323.end(); it++) {
        _s_402.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
        _s_407.push_back(_s_284._s_87(*it));
        _s_398[_s_284._s_87(*it)._s_68()] = _s_394;
        _s_399[_s_284._s_87(*it)._s_68()] = _s_284._s_87(*it);
        _s_400[_s_284._s_87(*it)._s_68()] = _s_284._s_88(*it);
    }
    for (std::vector<uint>::iterator it = _s_230.begin(); it!=_s_230.end(); it++) {
        _s_403.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
        _s_398[_s_284._s_87(*it)._s_68()] = _s_395;
        _s_399[_s_284._s_87(*it)._s_68()] = _s_284._s_87(*it);
        _s_400[_s_284._s_87(*it)._s_68()] = _s_284._s_88(*it);
    }
    for (std::vector<uint>::iterator it = _s_231.begin(); it!=_s_231.end(); it++) {
        _s_404.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
        _s_407.push_back(_s_284._s_87(*it));
        _s_398[_s_284._s_87(*it)._s_68()] = _s_396;
        _s_399[_s_284._s_87(*it)._s_68()] = _s_284._s_87(*it);
        _s_400[_s_284._s_87(*it)._s_68()] = _s_284._s_88(*it);
    }
    _s_29 _s_408 = mgr._s_38(_s_402);
    _s_28 _s_409 = mgr._s_36(_s_402);
    _s_29 _s_410 = mgr._s_38(_s_401);
    _s_28 _s_411 = mgr._s_36(_s_401);
    _s_28 _s_412 = mgr._s_36(_s_403);
    _s_28 _s_413 = mgr._s_36(_s_404);
    _s_28 _s_414 = mgr._s_36(_s_405);
    _s_28 _s_415 = mgr._s_36(_s_406);
    _s_28 _s_416 = mgr._s_36(_s_407);
    _s_27 _s_417 = _s_330;
    _s_27 _s_418 = mgr._s_34();
    while (_s_417!=_s_418) {
        _s_418 = _s_417;
        _s_417 |= (_s_417 & _s_324)._s_71(_s_414)._s_69(_s_410,_s_408);
    }
    std::cout << "# BDD Nodes original: " << _s_324._s_67() << "\n";
    _s_324 &= _s_331 & _s_331._s_69(_s_410,_s_408);
    std::cout << "# BDD Nodes now: " << _s_324._s_67() << "\n";
    std::set<uint> _s_419;
    std::tr1::unordered_map<uint,bool> _s_420;
    for (uint i=0; i<_s_322.size(); i++) {
        _s_27 var = _s_284._s_87(_s_322[i]);
        std::cout << "Test non essential variable: " << _s_284._s_88(_s_322[i]) << "\n";
        if (((_s_417 & var)==mgr._s_34()) || ((_s_417 & (!var))==mgr._s_34())) {
            std::cout << "Found non essential variable: " << _s_284._s_88(_s_322[i]) << "\n";
            _s_419.insert(_s_322[i]);
            _s_420[var._s_68()] = ((_s_417 & (!var))==mgr._s_34());
        }
    }
    std::cout << "---------NuSMV model:---------\n";
    std::cout << "MODULE main\n";
    std::cout << "IVAR\n";
    for (uint i=0; i<_s_230.size(); i++) {
        std::cout << "  " << _s_284._s_88(_s_230[i]) << " : boolean;\n";
    }
    std::cout << "VAR\n";
    for (uint i=0; i<_s_322.size(); i++) {
        if (_s_419.count(_s_322[i])==0) {
            std::cout << "  " << _s_284._s_88(_s_322[i]) << " : boolean;\n";
        }
    }
    for (uint i=0; i<_s_231.size(); i++) {
        std::cout << "  " << _s_284._s_88(_s_231[i]) << " : boolean;\n";
    }
    std::cout << "ASSIGN\n";
    for (uint i=0; i<_s_322.size(); i++) {
        int _s_421 = _s_322[i];
        if (_s_419.count(_s_322[i])==0) {
            _s_27 _s_422 = _s_284._s_87(_s_421);
            std::cout << "  init(" << _s_284._s_88(_s_421) << ") := ";
            if ((_s_422 & _s_330)==mgr._s_34()) {
                std::cout << "0;\n";
            } else if (((!_s_422) & _s_330)==mgr._s_34()) {
                std::cout << "1;\n";
            } else {
                std::cout << "0; -- Does not matter\n";
            }
        }
    }
    std::tr1::unordered_map<_s_27,_s_391> _s_423;
    std::tr1::unordered_map<_s_27,int> _s_424;
    {
        std::tr1::unordered_set<_s_27> _s_425;
        std::tr1::unordered_set<_s_27> _s_426;
        _s_425.insert(_s_324);
        _s_423[_s_324] = _s_391();
        while (_s_425.size()>0) {
            _s_27 _s_422 = *(_s_425.begin());
            _s_425.erase(_s_425.begin());
            _s_426.insert(_s_422);
            _s_424[_s_422] = _s_424.size();
            if (!_s_422._s_66()) {
                assert(_s_398.count(_s_422._s_68())>0);
                _s_27 _s_427 = (_s_422 & _s_399[_s_422._s_68()])._s_72(_s_399[_s_422._s_68()]);
                _s_27 _s_428 = (_s_422 & (!_s_399[_s_422._s_68()]))._s_72(_s_399[_s_422._s_68()]);
                _s_423[_s_427]._s_392.insert(std::pair<bool,_s_27>(true,_s_422));
                _s_423[_s_428]._s_392.insert(std::pair<bool,_s_27>(false,_s_422));
                if (_s_426.count(_s_427)==0) _s_425.insert(_s_427);
                if (_s_426.count(_s_428)==0) _s_425.insert(_s_428);
            }
        }
    }
    std::tr1::unordered_set<_s_27> _s_425;
    std::tr1::unordered_set<_s_27> _s_426;
    _s_425.insert(_s_324);
    std::cout << "DEFINE\n";
    bool _s_568 = false;
    while (_s_425.size()>0) {
        _s_27 _s_422 = *_s_425.begin();
        _s_425.erase(_s_425.begin());
        if (_s_426.count(_s_422)==0) {
            _s_426.insert(_s_422);
            std::tr1::unordered_set<std::pair<bool,_s_27> > &in = _s_423[_s_422]._s_392;
            if (in.size()==0) {
                std::cout << " firstPass_" << _s_424[_s_422] << "_in:= 1;\n";
                if (_s_568) throw "Already had initial node here!";
                _s_568 = true;
            } else {
                std::cout << " firstPass_" << _s_424[_s_422] << "_in:= 0";
                for (std::tr1::unordered_set<std::pair<bool,_s_27> >::iterator it = in.begin(); it!=in.end(); it++) {
                    std::cout << " | firstPass_" << _s_424[it->second] << "_" << (it->first?1:0);
                }
                std::cout << ";\n";
            }
            if (!_s_422._s_66()) {
                _s_27 _s_427 = (_s_422 & _s_399[_s_422._s_68()])._s_72(_s_399[_s_422._s_68()]);
                _s_27 _s_428 = (_s_422 & (!_s_399[_s_422._s_68()]))._s_72(_s_399[_s_422._s_68()]);
                _s_425.insert(_s_427);
                _s_425.insert(_s_428);
                assert(_s_398.count(_s_422._s_68())>0);
                switch (_s_398[_s_422._s_68()]) {
                case _s_396:
                case _s_394:
                    std::cout << " firstPass_" << _s_424[_s_422] << "_0:= firstPass_" << _s_424[_s_422] << "_in;\n";
                    std::cout << " firstPass_" << _s_424[_s_422] << "_1:= firstPass_" << _s_424[_s_422] << "_in;\n";
                    break;
                case _s_395:
                case _s_393:
                    if (_s_420.count(_s_422._s_68())>0) {
                        if (_s_420[_s_422._s_68()]) {
                            std::cout << " firstPass_" << _s_424[_s_422] << "_0:= 0;\n";
                            std::cout << " firstPass_" << _s_424[_s_422] << "_1:= firstPass_" << _s_424[_s_422] << "_in;\n";
                        } else {
                            std::cout << " firstPass_" << _s_424[_s_422] << "_1:= 0;\n";
                            std::cout << " firstPass_" << _s_424[_s_422] << "_0:= firstPass_" << _s_424[_s_422] << "_in;\n";
                        }
                    } else {
                        std::cout << " firstPass_" << _s_424[_s_422] << "_0:= firstPass_" << _s_424[_s_422] << "_in & !" << _s_400[_s_422._s_68()] << ";\n";
                        std::cout << " firstPass_" << _s_424[_s_422] << "_1:= firstPass_" << _s_424[_s_422] << "_in & " << _s_400[_s_422._s_68()] << ";\n";
                    }
                    break;
                }
            }
        }
    }
    _s_425.clear();
    _s_426.clear();
    _s_425.insert(mgr._s_33());
    while (_s_425.size()>0) {
        _s_27 _s_422 = *(_s_425.begin());
        _s_425.erase(_s_425.begin());
        _s_426.insert(_s_422);
        if (_s_422==mgr._s_33()) {
            std::cout << " secondPass_" << _s_424[_s_422] << " := 1;\n";
        } else {
            std::cout << " secondPass_" << _s_424[_s_422] << " := 0";
            _s_27 _s_427 = (_s_422 & _s_399[_s_422._s_68()])._s_72(_s_399[_s_422._s_68()]);
            _s_27 _s_428 = (_s_422 & (!_s_399[_s_422._s_68()]))._s_72(_s_399[_s_422._s_68()]);
            if (_s_427!=mgr._s_34()) {
                std::cout << " | secondPass_" << _s_424[_s_422] << "_" << _s_424[_s_427];
            }
            if (_s_428!=mgr._s_34()) {
                std::cout << " | secondPass_" << _s_424[_s_422] << "_" << _s_424[_s_428];
            }
            std::cout << ";\n";
        }
        std::ostringstream _s_430;
        _s_430 << " secondPass_" << _s_424[_s_422];
        std::tr1::unordered_set<std::pair<bool,_s_27> > &_s_392 = _s_423[_s_422]._s_392;
        for (std::tr1::unordered_set<std::pair<bool,_s_27> >::iterator it = _s_392.begin(); it!=_s_392.end(); it++) {
            std::cout << " secondPass_" << _s_424[it->second] << "_" << _s_424[_s_422] << " := ";
            std::cout << _s_430.str() << " & firstPass_" << _s_424[it->second] << "_" << (it->first?"1":"0") << ";\n";
            _s_430 << " & (!firstPass_" << _s_424[it->second] << "_" << (it->first?"1":"0") << ")";
            if (_s_426.count(it->second)==0) _s_425.insert(it->second);
        }
    }
    std::cout << "ASSIGN\n";
    for (uint i=0; i<_s_231.size(); i++) {
        int _s_432 = _s_231[i];
        _s_27 _s_431 = _s_284._s_87(_s_432);
        std::cout << "  next(" << _s_284._s_88(_s_231[i]) << ") := 0";
        for (std::tr1::unordered_map<_s_27,int>::iterator it = _s_424.begin(); it!=_s_424.end(); it++) {
            _s_27 _s_422 = it->first;
            if (_s_422._s_68()==_s_431._s_68()) {
                _s_27 _s_427 = (_s_422 & _s_399[_s_422._s_68()])._s_72(_s_399[_s_422._s_68()]);
                if (_s_427!=mgr._s_34()) {
                    std::cout << " | secondPass_" << _s_424[_s_422] << "_" << _s_424[_s_427];
                }
            }
        }
        std::cout << ";\n";
    }
    for (uint i=0; i<_s_323.size(); i++) {
        int _s_432 = _s_323[i];
        if (_s_419.count(_s_322[i])==0) {
            _s_27 _s_431 = _s_284._s_87(_s_432);
            std::cout << "  next(" << _s_284._s_88(_s_322[i]) << ") := 0";
            for (std::tr1::unordered_map<_s_27,int>::iterator it = _s_424.begin(); it!=_s_424.end(); it++) {
                _s_27 _s_422 = it->first;
                if (_s_422._s_68()==_s_431._s_68()) {
                    _s_27 _s_427 = (_s_422 & _s_399[_s_422._s_68()])._s_72(_s_399[_s_422._s_68()]);
                    if (_s_427!=mgr._s_34()) {
                        std::cout << " | secondPass_" << _s_424[_s_422] << "_" << _s_424[_s_427];
                    }
                }
            }
            std::cout << ";\n";
        }
    }
    std::cout << "\n\n";
    const _s_199 &_s_228 = _s_284._s_281()._s_236();
    std::vector<_s_189> _s_433;
    std::vector<_s_189> _s_342;
    _s_228._s_217(_s_433);
    _s_228._s_216(_s_342);
    for (std::vector<_s_189>::iterator it = _s_342.begin(); it!=_s_342.end(); it++) {
        std::cout << "LTLSPEC ";
        if (_s_433.size()>0) {
            std::cout << "(";
            bool first = true;
            for (std::vector<_s_189>::iterator it2 = _s_433.begin(); it2!=_s_433.end(); it2++) {
                if (first) {
                    first = false;
                } else {
                    std::cout << " & ";
                }
                std::cout << "(";
                _s_320(&(*it2),_s_284);
                std::cout << ")";
            }
            std::cout << ") -> ";
        }
        std::cout << "(";
        _s_320(&(*it),_s_284);
        std::cout << ")\n";
    }
}
void _s_320(const _s_189 *n, _s_225 &_s_284) {
    const std::vector<_s_189> _s_190 = n->_s_195();
    switch (n->_s_196()) {
    case _s_178:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) std::cout << " & ";
            std::cout << "(";
            _s_320(&(_s_190[i]),_s_284);
            std::cout << ")";
        }
        break;
    case _s_179:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) std::cout << " | ";
            std::cout << "(";
            _s_320(&(_s_190[i]),_s_284);
            std::cout << ")";
        }
        break;
    case _s_180:
        std::cout << "!(";
        _s_320(&(_s_190[0]),_s_284);
        std::cout << ")";
        break;
    case _s_181:
        std::cout << "X(";
        _s_320(&(_s_190[0]),_s_284);
        std::cout << ")";
        break;
    case _s_182:
    {
        const std::string _s_191 = n->_s_194();
        for (int i=0; i>=0; i++) {
            if ((_s_284._s_88(i)=="in_"+_s_191) || (_s_284._s_88(i)=="out_"+_s_191)) {
                if (_s_284._s_88(i)=="out_"+_s_191) {
                    std::cout << "X " << _s_284._s_88(i);
                } else {
                    std::cout << _s_284._s_88(i);
                }
                i = -2;
            }
        }
    }
    break;
    case _s_183:
        std::cout << "F(";
        _s_320(&(_s_190[0]),_s_284);
        std::cout << ")";
        break;
    case _s_184:
        std::cout << "G(";
        _s_320(&(_s_190[0]),_s_284);
        std::cout << ")";
        break;
    case _s_185:
        std::cout << "(";
        _s_320(&(_s_190[0]),_s_284);
        std::cout << ") U (";
        _s_320(&(_s_190[1]),_s_284);
        std::cout << ")";
        break;
    case _s_186:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) std::cout << " <-> ";
            std::cout << "(";
            _s_320(&(_s_190[i]),_s_284);
            std::cout << ")";
        }
        break;
    case _s_187:
        std::cout << "1";
        break;
    case _s_188:
        std::cout << "0";
        break;
    default:
        throw "Unhandled case.";
    }
}
void _s_605(const _s_189 *n, _s_225 &_s_284, std::ofstream &_s_8) {
    const std::vector<_s_189> _s_190 = n->_s_195();
    switch (n->_s_196()) {
    case _s_178:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) _s_8 << " & ";
            _s_8 << "(";
            _s_605(&(_s_190[i]),_s_284,_s_8);
            _s_8 << ")";
        }
        break;
    case _s_179:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) _s_8 << " | ";
            _s_8 << "(";
            _s_605(&(_s_190[i]),_s_284,_s_8);
            _s_8 << ")";
        }
        break;
    case _s_180:
        _s_8 << "!(";
        _s_605(&(_s_190[0]),_s_284,_s_8);
        _s_8 << ")";
        break;
    case _s_181:
        _s_8 << "X(";
        _s_605(&(_s_190[0]),_s_284,_s_8);
        _s_8 << ")";
        break;
    case _s_182:
    {
        const std::string _s_191 = n->_s_194();
        for (int i=0; i>=0; i++) {
            if ((_s_284._s_88(i)=="in_"+_s_191) || (_s_284._s_88(i)=="out_"+_s_191)) {
                if (_s_284._s_88(i)=="out_"+_s_191) {
                    _s_8 << "X " << _s_284._s_88(i);
                } else {
                    _s_8 << _s_284._s_88(i);
                }
                i = -2;
            }
        }
    }
    break;
    case _s_183:
        _s_8 << "F(";
        _s_605(&(_s_190[0]),_s_284,_s_8);
        _s_8 << ")";
        break;
    case _s_184:
        _s_8 << "G(";
        _s_605(&(_s_190[0]),_s_284,_s_8);
        _s_8 << ")";
        break;
    case _s_185:
        _s_8 << "(";
        _s_605(&(_s_190[0]),_s_284,_s_8);
        _s_8 << ") U (";
        _s_605(&(_s_190[1]),_s_284,_s_8);
        _s_8 << ")";
        break;
    case _s_186:
        for (uint i=0; i<_s_190.size(); i++) {
            if (i!=0) std::cout << " <-> ";
            _s_8 << "(";
            _s_605(&(_s_190[i]),_s_284,_s_8);
            _s_8 << ")";
        }
        break;
    case _s_187:
        _s_8 << "1";
        break;
    case _s_188:
        _s_8 << "0";
        break;
    default:
        throw "Unhandled case.";
    }
}
_s_332::_s_332(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329): _s_284(_s_310), _s_322(_s_325), _s_323(_s_326), _s_230(_s_327), _s_231(_s_328) {
    std::cout << "Extracting strategy: Computing combined transition relation...";
    std::cout.flush();
    _s_324 = _s_284._s_281()._s_245()._s_33();
    for (std::vector<_s_27>::const_iterator it = _s_329.begin(); it!=_s_329.end(); it++) {
        _s_324 &= *it;
    }
    std::cout << "done!" << std::endl;
}
void _s_332::_s_243(_s_27 _s_330, const _s_27 &_s_331) {
    BFBddManager &mgr = _s_284._s_281()._s_245();
    std::vector<_s_27> _s_401;
    std::vector<_s_27> _s_402;
    std::vector<_s_27> _s_403;
    std::vector<_s_27> _s_404;
    std::vector<_s_27> _s_405;
    std::vector<_s_27> _s_406;
    std::vector<_s_27> _s_407;
    for (std::vector<uint>::iterator it = _s_322.begin(); it!=_s_322.end(); it++) {
        _s_401.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_401.back());
    }
    for (std::vector<uint>::iterator it = _s_323.begin(); it!=_s_323.end(); it++) {
        _s_402.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
        _s_407.push_back(_s_284._s_87(*it));
    }
    for (std::vector<uint>::iterator it = _s_230.begin(); it!=_s_230.end(); it++) {
        _s_403.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
    }
    for (std::vector<uint>::iterator it = _s_231.begin(); it!=_s_231.end(); it++) {
        _s_404.push_back(_s_284._s_87(*it));
        _s_405.push_back(_s_284._s_87(*it));
        _s_406.push_back(_s_284._s_87(*it));
        _s_407.push_back(_s_284._s_87(*it));
    }
    _s_29 _s_408 = mgr._s_38(_s_402);
    _s_28 _s_409 = mgr._s_36(_s_402);
    _s_29 _s_410 = mgr._s_38(_s_401);
    _s_28 _s_411 = mgr._s_36(_s_401);
    _s_28 _s_412 = mgr._s_36(_s_403);
    _s_28 _s_413 = mgr._s_36(_s_404);
    _s_28 _s_414 = mgr._s_36(_s_405);
    _s_28 _s_415 = mgr._s_36(_s_406);
    _s_28 _s_416 = mgr._s_36(_s_407);
    std::cout << "Extracting strategy: Restricting transition relation...";
    std::cout.flush();
    _s_324 &= _s_331 & _s_331._s_69(_s_410,_s_408);
    std::cout << "done!" << std::endl;
    std::cout << "Extracting strategy: Making the transitions unique (state)...";
    std::cout.flush();
    for (uint i=0; i<_s_322.size(); i++) {
        _s_27 _s_434 = _s_284._s_87(_s_323[i]);
        _s_27 _s_435 = !((_s_324 & _s_434)._s_71(_s_409)._s_71(_s_413) & (_s_324 & (!_s_434))._s_71(_s_409)._s_71(_s_413));
        _s_435 |= !_s_434;
        _s_324 &= _s_435;
    }
    std::cout << "done!" << std::endl;
    std::cout << "Extracting strategy: Making the transitions unique (output)...";
    std::cout.flush();
    for (uint i=0; i<_s_231.size(); i++) {
        _s_27 _s_434 = _s_284._s_87(_s_231[i]);
        _s_27 _s_435 = !((_s_324 & _s_434)._s_71(_s_409)._s_71(_s_413) & (_s_324 & (!_s_434))._s_71(_s_409)._s_71(_s_413));
        _s_435 |= !_s_434;
        _s_324 &= _s_435;
    }
    std::cout << "done!" << std::endl;
    std::cout << "# Minterms Before restricting to reachable part of the state space: " << _s_324._s_57() << "\n";
    _s_27 _s_417 = _s_330;
    _s_27 _s_418 = mgr._s_34();
    while (_s_417!=_s_418) {
        std::cout << "# Minterms reachable : " << _s_417._s_57() << "\n";
        _s_418 = _s_417;
        _s_417 |= (_s_417 & _s_324)._s_71(_s_414)._s_69(_s_410,_s_408);
    }
    _s_324 &= _s_417 & _s_417._s_69(_s_410,_s_408);
    _s_324 &= _s_331 & _s_331._s_69(_s_410,_s_408);
    std::cout << "# Minterms now: " << _s_324._s_57() << "\n";
    std::set<uint> _s_419;
    for (uint i=0; i<_s_322.size(); i++) {
        _s_27 var = _s_284._s_87(_s_322[i]);
        std::cout << "Test non essential variable: " << _s_284._s_88(_s_322[i]) << "\n";
        if (((_s_417 & var)==mgr._s_34()) || ((_s_417 & (!var))==mgr._s_34())) {
            std::cout << "Found non essential variable: " << _s_284._s_88(_s_322[i]) << "\n";
            _s_419.insert(_s_322[i]);
        }
    }
    std::cout << "---------NuSMV model:---------\n";
    std::cout << "MODULE main\n";
    std::cout << "IVAR\n";
    for (uint i=0; i<_s_230.size(); i++) {
        std::cout << "  " << _s_284._s_88(_s_230[i]) << " : boolean;\n";
    }
    std::cout << "VAR\n";
    for (uint i=0; i<_s_322.size(); i++) {
        if (_s_419.count(_s_322[i])==0) {
            std::cout << "  " << _s_284._s_88(_s_322[i]) << " : boolean;\n";
        }
    }
    for (uint i=0; i<_s_231.size(); i++) {
        std::cout << "  " << _s_284._s_88(_s_231[i]) << " : boolean;\n";
    }
    std::cout << "ASSIGN\n";
    for (uint i=0; i<_s_322.size(); i++) {
        int _s_421 = _s_322[i];
        if (_s_419.count(_s_322[i])==0) {
            _s_27 _s_422 = _s_284._s_87(_s_421);
            std::cout << "  init(" << _s_284._s_88(_s_421) << ") := ";
            if ((_s_422 & _s_330)==mgr._s_34()) {
                std::cout << "0;\n";
            } else if (((!_s_422) & _s_330)==mgr._s_34()) {
                std::cout << "1;\n";
            } else {
                _s_330 &= !_s_422;
                std::cout << "0; -- Does not matter\n";
            }
        }
    }
    for (uint i=0; i<_s_323.size(); i++) {
        if (_s_419.count(_s_322[i])==0) {
            int _s_421 = _s_323[i];
            _s_27 _s_422 = _s_284._s_87(_s_421) & _s_324;
            std::cout << "  next(" << _s_284._s_88(_s_322[i]) << ") := case\n";
            std::tr1::unordered_set<std::string> _s_436;
            for (_s_27::_s_77 it(_s_422); it; it++) {
                std::ostringstream out;
                out << "      1";
                for (uint j=0; j<_s_322.size(); j++) {
                    int nr = _s_284._s_87(_s_322[j])._s_68();
                    if (_s_419.count(_s_322[j])==0) {
                        switch (it[nr]) {
                        case _s_27::_s_77::_s_78:
                            out << " & ";
                            out << _s_284._s_88(_s_322[j]);
                            break;
                        case _s_27::_s_77::_s_79:
                            out << " & ";
                            out << "!"+_s_284._s_88(_s_322[j]);
                            break;
                        default:
                            break;
                        }
                    }
                }
                for (uint j=0; j<_s_230.size(); j++) {
                    int nr = _s_284._s_87(_s_230[j])._s_68();
                    switch (it[nr]) {
                    case _s_27::_s_77::_s_78:
                        out << " & ";
                        out << _s_284._s_88(nr);
                        break;
                    case _s_27::_s_77::_s_79:
                        out << " & ";
                        out << "!"+_s_284._s_88(nr);
                        break;
                    default:
                        break;
                    }
                }
                out << " : 1;\n";
                _s_436.insert(out.str());
            }
            for (std::tr1::unordered_set<std::string>::iterator it = _s_436.begin(); it!=_s_436.end(); it++) {
                std::cout << *it;
            }
            std::cout << "      1 : 0;\n";
            std::cout << "    esac;\n";
        }
    }
    for (uint i=0; i<_s_231.size(); i++) {
        int _s_421 = _s_231[i];
        _s_27 _s_422 = _s_284._s_87(_s_421) & _s_324;
        std::cout << "  next(" << _s_284._s_88(_s_421) << ") := case\n";
        std::tr1::unordered_set<std::string> _s_436;
        for (_s_27::_s_77 it(_s_422); it; it++) {
            std::ostringstream out;
            out << ("      1");
            for (uint j=0; j<_s_322.size(); j++) {
                int nr = _s_284._s_87(_s_322[j])._s_68();
                if (_s_419.count(_s_322[j])==0) {
                    switch (it[nr]) {
                    case _s_27::_s_77::_s_78:
                        out << " & ";
                        out << _s_284._s_88(_s_322[j]);
                        break;
                    case _s_27::_s_77::_s_79:
                        out << " & ";
                        out << "!"+_s_284._s_88(_s_322[j]);
                        break;
                    default:
                        break;
                    }
                }
            }
            for (uint j=0; j<_s_230.size(); j++) {
                int nr = _s_284._s_87(_s_230[j])._s_68();
                switch (it[nr]) {
                case _s_27::_s_77::_s_78:
                    out << " & ";
                    out << _s_284._s_88(nr);
                    break;
                case _s_27::_s_77::_s_79:
                    out << " & ";
                    out << "!"+_s_284._s_88(nr);
                    break;
                default:
                    break;
                }
            }
            out << " : 1;\n";
            _s_436.insert(out.str());
        }
        for (std::tr1::unordered_set<std::string>::iterator it = _s_436.begin(); it!=_s_436.end(); it++) {
            std::cout << *it;
        }
        std::cout << "      1 : 0;\n";
        std::cout << "    esac;\n";
    }
    std::cout << "\n\n";
    const _s_199 &_s_228 = _s_284._s_281()._s_236();
    std::vector<_s_189> _s_433;
    std::vector<_s_189> _s_342;
    _s_228._s_217(_s_433);
    _s_228._s_216(_s_342);
    for (std::vector<_s_189>::iterator it = _s_342.begin(); it!=_s_342.end(); it++) {
        std::cout << "LTLSPEC ";
        if (_s_433.size()>0) {
            std::cout << "(";
            bool first = true;
            for (std::vector<_s_189>::iterator it2 = _s_433.begin(); it2!=_s_433.end(); it2++) {
                if (first) {
                    first = false;
                } else {
                    std::cout << " & ";
                }
                std::cout << "(";
                _s_320(&(*it2),_s_284);
                std::cout << ")";
            }
            std::cout << ") -> ";
        }
        std::cout << "(";
        _s_320(&(*it),_s_284);
        std::cout << ")\n";
    }
}
_s_598::_s_598(_s_225 &_s_310, const std::vector<uint> &_s_325, const std::vector<uint> &_s_326, const std::vector<uint> &_s_327, const std::vector<uint> &_s_328, const std::vector<_s_27> &_s_329, std::string _s_603): _s_284(_s_310), _s_322(_s_325), _s_323(_s_326), _s_230(_s_327), _s_231(_s_328), _s_604(_s_603) {
    std::cout << "Extracting strategy: Computing combined transition relation...";
    std::cout.flush();
    _s_324 = _s_284._s_281()._s_245()._s_33();
    for (std::vector<_s_27>::const_iterator it = _s_329.begin(); it!=_s_329.end(); it++) {
        _s_324 &= *it;
    }
    std::cout << "done!" << std::endl;
}
void _s_598::_s_243(_s_27 _s_330, const _s_27 &_s_331) {
    std::ofstream _s_607(_s_604.c_str());
    _s_607 << "FileInfo: non-deterministic strategy\n\nMealy";
    _s_607 << "\nVarsPre:\n";
    for (std::vector<uint>::iterator it = _s_322.begin(); it!=_s_322.end(); it++) {
        _s_607 << _s_284._s_88(*it) << "\n";
        _s_607 << _s_284._s_87(*it)._s_68() << "\n";
    }
    _s_607 << "\nVarsPost:\n";
    for (std::vector<uint>::iterator it = _s_323.begin(); it!=_s_323.end(); it++) {
        _s_607 << _s_284._s_88(*it) << "\n";
        _s_607 << _s_284._s_87(*it)._s_68() << "\n";
    }
    _s_607 << "\nVarsInput:\n";
    for (std::vector<uint>::iterator it = _s_230.begin(); it!=_s_230.end(); it++) {
        _s_607 << _s_284._s_88(*it) << "\n";
        _s_607 << _s_284._s_87(*it)._s_68() << "\n";
    }
    _s_607 << "\nVarsOutput:\n";
    for (std::vector<uint>::iterator it = _s_231.begin(); it!=_s_231.end(); it++) {
        _s_607 << _s_284._s_88(*it) << "\n";
        _s_607 << _s_284._s_87(*it)._s_68() << "\n";
    }
    _s_607 << "\nNuSMVSpec:\n";
    const _s_199 &_s_228 = _s_284._s_281()._s_236();
    std::vector<_s_189> _s_433;
    std::vector<_s_189> _s_342;
    _s_228._s_217(_s_433);
    _s_228._s_216(_s_342);
    for (std::vector<_s_189>::iterator it = _s_342.begin(); it!=_s_342.end(); it++) {
        _s_607 << "LTLSPEC ";
        if (_s_433.size()>0) {
            _s_607 << "(";
            bool first = true;
            for (std::vector<_s_189>::iterator it2 = _s_433.begin(); it2!=_s_433.end(); it2++) {
                if (first) {
                    first = false;
                } else {
                    _s_607 << " & ";
                }
                _s_607 << "(";
                _s_605(&(*it2),_s_284,_s_607);
                _s_607 << ")";
            }
            _s_607 << ") -> ";
        }
        _s_607 << "(";
        _s_605(&(*it),_s_284,_s_607);
        _s_607 << ")\n";
    }
    _s_607 << "\n";
    DdNode *toStore[3];
    toStore[2] = _s_324._s_608();
    toStore[1] = _s_330._s_608();
    toStore[0] = _s_331._s_608();
    _s_607 << 3 << "\n";
    if (_s_607.fail()) {
        std::cerr << "Error writing to: " << _s_604 << ".\n";
        std::exit(2);
    }
    _s_607.close();
    FILE *_s_606 = fopen(_s_604.c_str(),"a+");
    if (_s_606==NULL) {
        std::cerr << "Error reopening: " << _s_604 << ".\n";
        std::exit(2);
    }
    int _s_616 =
        Dddmp_cuddBddArrayStore(
            _s_284._s_282()._s_610(),
            NULL,
            3,
            toStore,
            NULL,
            NULL,
            NULL,
            DDDMP_VAR_MATCHIDS,
            DDDMP_VARDEFAULT,
            NULL,
            _s_606
        );
    if (_s_616 != DDDMP_SUCCESS) {
        std::cerr << "Error storing BDDs to file: " << _s_604 << ".\n";
        std::cerr << "Got error code: " << _s_616 << std::endl;
        std::exit(2);
    }
    fclose(_s_606);
}
_s_27 _s_226::_s_307(const _s_101 &_s_144, uint _s_52, uint to, _s_27 &_s_308) const {
    _s_27 _s_422 = _s_144._s_139(_s_52,to);
    _s_27 _s_437 = _s_99("safe");
    _s_422 = (_s_422 & _s_437 & _s_308) | (_s_422 & (!_s_437) & (!_s_308));
    _s_422 = _s_422._s_72(_s_437);
    return _s_422;
}
_s_226::_s_226(_s_227 &_s_564) : _s_283(_s_564), _s_284(NULL), _s_285(NULL), _s_286(NULL), _s_287(NULL), _s_288(NULL), _s_301(0) {
    _s_564._s_236()._s_216(_s_290);
    _s_564._s_236()._s_217(_s_291);
    _s_289 = _s_283._s_245()._s_35();
}
_s_226::~_s_226() {
    if (_s_285!=NULL) delete _s_285;
    if (_s_286!=NULL) delete _s_286;
    if (_s_287!=NULL) delete _s_287;
    if (_s_288!=NULL) delete _s_288;
}
std::string _s_226::_s_292(_s_189 root) {
    switch (root._s_196()) {
    case _s_179:
    {
        std::ostringstream out;
        out << "( ";
        for (uint i=0; i<root._s_195().size(); i++) {
            if (i!=0) out << " || ";
            out << _s_292(root._s_195()[i]);
        }
        out << " )";
        return out.str();
    }
    break;
    case _s_187:
    {
        return "true";
    }
    break;
    case _s_188:
    {
        return "false";
    }
    break;
    case _s_178:
    {
        std::ostringstream out;
        out << "( ";
        for (uint i=0; i<root._s_195().size(); i++) {
            if (i!=0) out << " && ";
            out << _s_292(root._s_195()[i]);
        }
        out << " )";
        return out.str();
    }
    break;
    case _s_181:
    {
        std::ostringstream out;
        out << "( X " << _s_292(root._s_195()[0]);
        out << " )";
        return out.str();
    }
    break;
    case _s_184:
    {
        std::ostringstream out;
        out << "( [] " << _s_292(root._s_195()[0]);
        out << " )";
        return out.str();
    }
    break;
    case _s_180:
    {
        std::ostringstream out;
        out << "( ! " << _s_292(root._s_195()[0]);
        out << " )";
        return out.str();
    }
    break;
    case _s_183:
    {
        std::ostringstream out;
        out << "( <> " << _s_292(root._s_195()[0]);
        out << " )";
        return out.str();
    }
    break;
    case _s_185:
    {
        std::ostringstream out;
        out << "( " << _s_292(root._s_195()[0]);
        out << " U ";
        out << _s_292(root._s_195()[1]);
        out << " )";
        return out.str();
    }
    break;
    case _s_186:
    {
        std::ostringstream out;
        out << "( " << _s_292(root._s_195()[0]);
        out << " <-> ";
        out << _s_292(root._s_195()[1]);
        out << " )";
        return out.str();
    }
    break;
    case _s_182:
    {
        const std::string &_s_191 = root._s_194();
        std::ostringstream out;
        out << "v" << _s_283._s_246(_s_191);
        return out.str();
    }
    break;
    default:
        throw _s_2("LTL compiler (createLTLSpecification): Unknown node type");
    }
}
void _s_226::_s_305(std::string _s_438, _s_101 &_s_144) {
    std::cout << "LTL String: " << _s_438 << std::endl;
    std::string _s_439;
    _s_283._s_236()._s_387(_s_439);
    std::string _s_442 = _s_439 + " '! ( "+_s_438+" )'";
    _s_6 _s_440(_s_442.c_str());
    boost::iostreams::stream<boost::iostreams::file_descriptor_source> *_s_441 = _s_440._s_9();
    std::cout << "Executing LTL compiler: " << _s_442 << std::endl;
    std::ostringstream _s_443;
    while (!_s_441->eof()) {
        std::string out;
        std::getline(*_s_441,out);
        _s_443 << out << std::endl;
        std::cout << "O: " << out << std::endl;
    }
    _s_101::_s_143(_s_144,_s_443.str());
    _s_144._s_121();
}
void _s_226::_s_309(_s_225 *_s_310) {
    _s_284 = _s_310;
    _s_340 _s_448(*_s_284);
    std::vector<std::string> _s_444;
    std::vector<std::string> _s_445;
    std::vector<std::string> _s_446;
    std::vector<std::string> _s_447;
    for (uint i=0; i<_s_290.size(); i++) {
        std::cout << "Check limited safety....\n";
        if (_s_448._s_357(_s_290[i])) {
            std::cout << "Limited lookback!\n";
            _s_448._s_359(_s_290[i]);
        } else {
            std::string _s_306 = _s_292(_s_290[i]);
            _s_101 _s_126(this);
            _s_305(_s_306,_s_126);
            if (_s_126._s_124()) {
                _s_444.push_back(_s_306);
            } else {
                _s_445.push_back(_s_306);
            }
        }
    }
    for (uint i=0; i<_s_291.size(); i++) {
        std::string _s_306 = _s_292(_s_291[i]);
        std::cout << "Checking if the following is a safety assumption: " << _s_306 << "\n";
        _s_101 _s_126(this);
        _s_305(_s_306,_s_126);
        _s_126._s_130();
        if (_s_126._s_124()) {
            std::cout << "Identified safety assumption.\n";
            _s_446.push_back(_s_306);
        } else {
            std::cout << "Identified liveness assumption.\n";
            _s_447.push_back(_s_306);
        }
    }
    std::cout << "=== Generating combined safety specs === " << std::endl;
    if (_s_444.size() > 0) {
        std::ostringstream _s_358;
        for (uint i=0; i<_s_444.size(); i++) {
            std::string _s_306 = _s_444[i];
            if (i>0) _s_358 << " && ";
            _s_358 << "( "<< _s_306 << " )";
        }
        _s_295.push_back(_s_101(this));
        _s_101 &_s_126 = _s_295.back();
        _s_305(_s_358.str(),_s_126);
        if (!_s_126._s_124()) {
            throw "Error: Combined safety spec is no safety spec anymore!";
        }
    }
    std::cout << "=== Generating combined safety assumptions === " << std::endl;
    if (_s_446.size()>0) {
        std::ostringstream _s_358;
        for (uint i=0; i<_s_446.size(); i++) {
            std::string _s_306 = _s_446[i];
            if (i>0) _s_358 << " && ";
            _s_358 << "( "<< _s_306 << " )";
        }
        _s_295.push_back(_s_101(this));
        _s_305(_s_358.str(),_s_295.back());
        _s_295.back()._s_130();
        _s_296.insert(&_s_295.back());
        if (!_s_295.back()._s_124()) throw "Error: Incorrectly classified some assumption as safety or the LTL-to-Buechi converter made the resulting automaton needlessly complicated (such that it syntactically looks like a liveness property, which is unsupported behaviour!";
    }
    std::cout << "=== Generating combined liveness specs === " << std::endl;
    std::string _s_449;
    {
        std::ostringstream _s_378;
        _s_378 << "( true ";
        for (uint i=0; i<_s_447.size(); i++) {
            _s_378 << "&& ( " << _s_447[i] << " ) ";
        }
        _s_378 << " ) ";
        _s_449 = _s_378.str();
    }
    {
        std::ostringstream _s_358;
        _s_358 << "( ! ( " << _s_449 << " ) ) || ( ";
        for (uint i=0; i<_s_445.size(); i++) {
            std::string _s_306 = _s_445[i];
            _s_358 << "( "<< _s_306 << ")";
            _s_358 << " && ";
        }
        _s_358 << " ( [] safe ) ";
        _s_358 << " ) ";
        _s_101 _s_450(this);
        if (_s_283._s_236()._s_218()) {
            _s_305("! ("+_s_358.str()+ " )",_s_450);
        } else {
            _s_305(_s_358.str(),_s_450);
        }
        _s_293.push_back(_s_101(this));
        _s_450._s_130();
        _s_293.back() = _s_450;
        _s_294.push_back(std::vector<bool>());
        if (!(_s_283._s_236()._s_224())) {
            _s_293.back()._s_1(_s_294.back());
        } else {
            for (uint i=0; i<_s_293.back()._s_112(); i++) {
                _s_294.back().push_back(false);
            }
        }
    }
    if (_s_448._s_363()) {
        _s_448._s_243(_s_281()._s_236()._s_218());
        _s_286 = new _s_27(_s_448._s_360() & _s_448._s_362());
        _s_287 = new _s_27(_s_448._s_361());
        _s_288 = new _s_27(_s_448._s_316());
    }
}
void _s_226::_s_311(uint _s_312) {
    if (_s_312<=_s_301) throw _s_2("LTLCompiler::increaseNofCounterVars - newNumber is not higher than nofCounterVarsSoFar");
    std::cout << "LTL Synthesis: New number of variables per state: " << _s_312 << std::endl;
    uint _s_451 = _s_301;
    uint _s_452 = _s_312 - _s_301;
    _s_301 = _s_312;
    for (uint i=0; i<_s_293.size(); i++) {
        for (uint k=0; k<_s_452; k++) {
            std::cout << "Adding variables..." << std::endl;
            _s_101 &_s_144 = _s_293[i];
            std::vector<_s_27> _s_617;
            if (_s_297.size()==i) _s_297.push_back(std::vector< std::vector< uint > >());
            if (_s_298.size()==i) _s_298.push_back(std::vector< std::vector< uint > >());
            for (uint j=0; j<_s_144._s_112(); j++) {
                if (_s_297[i].size()==j) _s_297[i].push_back(std::vector< uint >());
                if (_s_298[i].size()==j) _s_298[i].push_back(std::vector< uint >());
                if ((_s_297[i][j].size()==0) || (!_s_294[i][j])) {
                    std::ostringstream _s_191;
                    _s_191 << "LTL_Spec" << i << _s_144._s_118(j) << "_NALevel" << (_s_451+k);
                    uint _s_453 = _s_284->_s_247(_s_191.str());
                    uint _s_571 = _s_284->_s_248(_s_191.str()+"'");
                    _s_297[i][j].push_back(_s_453);
                    _s_298[i][j].push_back(_s_571);
                    _s_617.push_back(_s_284->_s_87(_s_453));
                    _s_617.push_back(_s_284->_s_87(_s_571));
                    _s_284->_s_282()._s_41(_s_617);
                    _s_617.clear();
                }
            }
        }
    }
    if (_s_452==_s_301) {
        for (uint i=0; i<_s_295.size(); i++) {
            _s_101 &_s_144 = _s_295[i];
            if (_s_299.size()==i) _s_299.push_back(std::vector< uint >());
            if (_s_300.size()==i) _s_300.push_back(std::vector< uint >());
            for (uint j=0; j<_s_144._s_112(); j++) {
                std::ostringstream _s_191;
                _s_191 << "Safety_Spec" << i << _s_144._s_118(j);
                uint _s_453 = _s_284->_s_247(_s_191.str());
                uint _s_571 = _s_284->_s_248(_s_191.str()+"'");
                _s_299[i].push_back(_s_453);
                _s_300[i].push_back(_s_571);
            }
        }
    }
}
_s_27 _s_226::_s_302(const std::vector<uint> &first, const std::vector<uint> &second) const {
    if ((first.size()==1) || (second.size()==1)) {
        return _s_284->_s_87(first[0]) | !_s_284->_s_87(second[0]);
    }
    assert(first.size()==second.size());
    _s_27 _s_59 = _s_283._s_245()._s_33();
    for (uint i=1; i<first.size(); i++) {
        _s_27 a = _s_284->_s_87(first[i]);
        _s_27 b = _s_284->_s_87(second[i]);
        _s_59 = ((!a) & (b)) | (((((a)) & (b)) | ((!a) & (!(b)))) & _s_59);
    }
    _s_27 a = _s_284->_s_87(first[0]);
    _s_27 b = _s_284->_s_87(second[0]);
    _s_59 = (_s_59 & a & b) | !b;
    return _s_59;
}
_s_27 _s_226::_s_303(const std::vector<uint> &first, const std::vector<uint> &second) const {
    if ((first.size()==1) || (second.size()==1)) {
        return _s_284->_s_87(first[0]) | !_s_284->_s_87(second[0]);
    }
    _s_27 _s_59 = _s_302(first,second);
    _s_27 _s_455 = _s_283._s_245()._s_34();
    for (uint i=1; i<first.size(); i++) {
        _s_455 |= (((!(_s_284->_s_87(first[i]))) & (_s_284->_s_87(second[i]))) | ((_s_284->_s_87(first[i])) & (!(_s_284->_s_87(second[i])))));
    }
    _s_59 &= _s_455 & _s_284->_s_87(first[0]) & _s_284->_s_87(second[0]);
    _s_59 |= !_s_284->_s_87(second[0]);
    return _s_59;
}
_s_27 _s_226::_s_304(const std::vector<uint> &first) const {
    return _s_284->_s_87(first[0]);
}
_s_27 _s_226::_s_316() const {
    _s_27 _s_59 = _s_283._s_245()._s_33();
    for (uint i=0; i<_s_293.size(); i++) {
        const _s_101 &_s_144 = _s_293[i];
        for (uint j=0; j<_s_144._s_112(); j++) {
            std::cout << "liveness: nba.getStateName(j): " << _s_144._s_118(j) << "\n";
            std::string _s_456 = _s_144._s_118(j);
            if (_s_456.substr(_s_456.length()-4)=="init") {
                for (uint k=0; k<_s_297[i][j].size(); k++) {
                    _s_59 &= _s_284->_s_87(_s_297[i][j][k]);
                }
            } else {
                for (uint k=0; k<_s_297[i][j].size(); k++) {
                    _s_59 &= !(_s_284->_s_87(_s_297[i][j][k]));
                }
            }
        }
    }
    for (uint i=0; i<_s_295.size(); i++) {
        const _s_101 &_s_144 = _s_295[i];
        for (uint j=0; j<_s_144._s_112(); j++) {
            std::cout << "safety: nba.getStateName(j): " << _s_144._s_118(j) << "\n";
            std::string _s_456 = _s_144._s_118(j);
            if (_s_456.substr(_s_456.length()-4)=="init") {
                _s_59 &= _s_284->_s_87(_s_299[i][j]);
            } else {
                _s_59 &= !(_s_284->_s_87(_s_299[i][j]));
            }
        }
    }
    if (_s_287!=NULL) _s_59 &= *_s_287;
    if (_s_288!=NULL) _s_59 &= *_s_288;
    return _s_59;
}
_s_27 _s_226::_s_315() const {
    _s_27 _s_457 = _s_283._s_245()._s_34();
    for (uint i=0; i<_s_295.size(); i++) {
        const _s_101 &_s_422 = _s_295[i];
        if (_s_296.count(&_s_422)==0) {
        } else {
            for (uint j=0; j<_s_422._s_112(); j++) {
                if (_s_422._s_119(j)) {
                    int nof = _s_299[i][j];
                    std::cout.flush();
                    _s_457 |= _s_284->_s_87(nof);
                }
            }
        }
    }
    return _s_457;
}
void _s_226::_s_313(std::vector<_s_27> &_s_314) {
    if (_s_301<=1)
        assert(_s_301>=2);
    _s_27 _s_308 = _s_317();
    for (uint _s_458=0; _s_458<_s_293.size(); _s_458++) {
        const _s_101 &_s_422 = _s_293[_s_458];
        _s_27 _s_459 = _s_283._s_245()._s_33();
        for (uint j=0; j<_s_422._s_112(); j++) {
            for (uint k=0; k<_s_422._s_112(); k++) {
                _s_27 _s_460 = _s_307(_s_422,k,j,_s_308);
                _s_27 _s_461 = !_s_460;
                _s_27 _s_462 = (_s_422._s_119(j))?(_s_303(_s_298[_s_458][j],_s_297[_s_458][k])):(_s_302(_s_298[_s_458][j],_s_297[_s_458][k]));
                _s_459 &= _s_461 | _s_462;
            }
            _s_314.push_back(_s_459);
            _s_459 = _s_283._s_245()._s_33();
        }
    }
    if (_s_285==NULL) {
        _s_27 _s_464 = _s_283._s_245()._s_33();
        for (uint _s_458=0; _s_458<_s_295.size(); _s_458++) {
            const _s_101 &_s_422 = _s_295[_s_458];
            bool _s_463 = _s_296.count(&_s_422)>0;
            _s_27 _s_459 = _s_283._s_245()._s_33();
            if ((!_s_463) ^ _s_283._s_236()._s_218()) {
                for (uint j=0; j<_s_422._s_112(); j++) {
                    for (uint k=0; k<_s_422._s_112(); k++) {
                        _s_27 _s_460 = _s_307(_s_422,k,j,_s_308);
                        _s_27 _s_461 = !_s_460;
                        _s_27 _s_462 = (_s_284->_s_87(_s_300[_s_458][j]) | (!_s_284->_s_87(_s_299[_s_458][k])));
                        _s_459 &= _s_461 | _s_462;
                    }
                }
            } else {
                for (uint j=0; j<_s_422._s_112(); j++) {
                    _s_27 _s_465 = _s_283._s_245()._s_34();
                    for (uint k=0; k<_s_422._s_112(); k++) {
                        _s_27 _s_460 = _s_307(_s_422,k,j,_s_308);
                        _s_465 |= _s_460 & (_s_284->_s_87(_s_299[_s_458][k]));
                    }
                    _s_459 &= (!_s_284->_s_87(_s_300[_s_458][j])) | _s_465;
                }
            }
            _s_464 &= _s_459;
        }
        _s_285 = new _s_27(_s_464);
    }
    _s_314.push_back(*_s_285);
    if (_s_286!=NULL) _s_314.push_back(*_s_286);
}
_s_28 _s_226::_s_36(const std::vector<uint> &_s_467) const {
    uint _s_466 = _s_467.size();
    _s_27 *cube = new _s_27[_s_466];
    int *_s_37 = new int[_s_466];
    for (uint nr = 0; nr < _s_466; nr++) {
        cube[nr] = _s_284->_s_87(_s_467[nr]);
        _s_37[nr] = 1;
    }
    _s_28 _s_59 = _s_283._s_245()._s_36(cube,_s_37,_s_466);
    delete[] _s_37;
    delete[] cube;
    return _s_59;
}
_s_27 _s_226::_s_317() {
    _s_27 safe = _s_283._s_245()._s_33();
    for (uint i=0; i<_s_295.size(); i++) {
        const _s_101 &_s_422 = _s_295[i];
        if (_s_296.count(&_s_422)==0) {
            for (uint j=0; j<_s_422._s_112(); j++) {
                if (_s_422._s_119(j)) {
                    int nof = _s_300[i][j];
                    std::cout << "getNotLosingPostStates, nof=" << nof << std::endl;
                    std::cout.flush();
                    safe &= !_s_284->_s_87(nof);
                }
            }
        }
    }
    if (_s_287!=NULL) safe &= *_s_287;
    return safe;
}
_s_27 _s_226::_s_99(std::string name) const {
    if (name[0]=='v') {
        std::istringstream num(name.substr(1,std::string::npos));
        uint _s_468;
        num >> _s_468;
        if (num.fail()) {
            throw _s_2("Internal Error (LTLCompiler): No variable number: " + name);
        }
        return _s_283._s_87(_s_468);
    } else if (name=="safe") {
        return _s_289;
    }
    throw _s_2("Internal Error (LTLCompiler) - BddNBA_getVar(std::string name) : Variable not found: " + name);
}
int operator<(const _s_333& _s_338, const _s_333& _s_339) {
    if (_s_338._s_191!=_s_339._s_191) return _s_338._s_191<_s_339._s_191;
    return _s_338._s_334<_s_339._s_334;
}
int operator==(const _s_333& _s_338, const _s_333& _s_339) {
    if (_s_338._s_191!=_s_339._s_191) return _s_338._s_191==_s_339._s_191;
    return _s_338._s_334==_s_339._s_334;
}
_s_340::_s_340(_s_225 &_s_310) : _s_284(_s_310), mgr(_s_310._s_282()) {
};
_s_340::~_s_340() {
}
int _s_340::_s_351(const _s_189 &n, int _s_352, std::set<_s_333> &_s_96) {
    switch (n._s_196()) {
    case _s_180:
    case _s_179:
    case _s_186:
    case _s_178:
    {
        const std::vector<_s_189> &_s_83 = n._s_195();
        int max = -1;
        for (uint i=0; i<_s_83.size(); i++)
            max = ((max<_s_351(_s_83[i],_s_352, _s_96))?(_s_351(_s_83[i],_s_352, _s_96)):(max));
        return max;
    }
    break;
    case _s_181:
    {
        const std::vector<_s_189> &_s_83 = n._s_195();
        for (uint i=0; i<_s_83.size(); i++)
            return _s_351(_s_83[i],_s_352+1, _s_96);
        {
            std::cerr << "Fatal error in line " << 5851 << " in file " << "/tmp/all.cpp" << ": " << "Next nodes without children." << "\n";
            exit(1);
            throw "Failed to exit(1) the program properly!";
        };
    }
    break;
    case _s_182:
    {
        const std::string &_s_191 = n._s_194();
        _s_96.insert(_s_333(_s_352,_s_191));
        return _s_352;
    }
    break;
    case _s_187:
    case _s_188:
        return -1;
    default:
    {
        std::cerr << "Fatal error in line " << 5865 << " in file " << "/tmp/all.cpp" << ": " << "What's this? (SafetyCompiler.cpp)" << "\n";
        exit(1);
        throw "Failed to exit(1) the program properly!";
    };
    }
}
std::string _s_469(std::string _s_165, uint value) {
    std::ostringstream o;
    o << _s_165 << "_" << value;
    return o.str();
}
_s_27 _s_340::_s_353(const _s_189 &n, int _s_352, int _s_354, std::string _s_355) {
    _s_27 _s_59;
    switch (n._s_196()) {
    case _s_180:
    {
        const std::vector<_s_189> &_s_83 = n._s_195();
        assert(_s_83.size()==1);
        _s_59 = !_s_353(_s_83[0],_s_352, _s_354, _s_469(_s_355,0));
    }
    break;
    case _s_179:
    {
        _s_59 = mgr._s_34();
        const std::vector<_s_189> &_s_83 = n._s_195();
        for (uint i=0; i<_s_83.size(); i++)
            _s_59 |= _s_353(_s_83[i],_s_352, _s_354,_s_469(_s_355,i));
    }
    break;
    case _s_186:
    {
        _s_59 = mgr._s_33();
        const std::vector<_s_189> &_s_83 = n._s_195();
        assert(_s_83.size()==2);
        _s_27 one = _s_353(_s_83[0],_s_352, _s_354,_s_469(_s_355,0));
        _s_27 two = _s_353(_s_83[1],_s_352, _s_354,_s_469(_s_355,1));
        _s_59 = (one&two) | ((!one) & (!two));
    }
    break;
    case _s_178:
    {
        _s_59 = mgr._s_33();
        const std::vector<_s_189> &_s_83 = n._s_195();
        for (uint i=0; i<_s_83.size(); i++)
            _s_59 &= _s_353(_s_83[i],_s_352, _s_354,_s_469(_s_355,i));
    }
    break;
    case _s_181:
    {
        const std::vector<_s_189> &_s_83 = n._s_195();
        assert(_s_83.size()==1);
        _s_59 = _s_353(_s_83[0],_s_352+1, _s_354,_s_469(_s_355,0));
    }
    break;
    case _s_182:
    {
        const std::string &_s_191 = n._s_194();
        uint _s_334 = _s_354 - _s_352;
        uint _s_470 = _s_346.at(std::pair<uint,std::string>(_s_334,_s_191)).first;
        std::cout << "Var (Prefix:" << _s_355 << "): " << _s_191 << "(" << _s_334 << "): " << _s_470 << std::endl;
        _s_59 = _s_284._s_87(_s_470);
    }
    break;
    case _s_187:
        _s_59 = mgr._s_33();
        break;
    case _s_188:
        _s_59 = mgr._s_34();
        break;
    default:
        throw _s_2("What's this? (SafetyCompiler.cpp)",5940);
    }
    return _s_59;
}
uint _s_227::_s_247(const std::string &_s_191) {
    assert(vars.size()==_s_229.size());
    vars.push_back(mgr._s_35());
    int nr = vars.size()-1;
    _s_229.push_back(_s_191);
    _s_232.push_back(nr);
    return nr;
}
uint _s_227::_s_248(const std::string &_s_191) {
    assert(vars.size()==_s_229.size());
    vars.push_back(mgr._s_35());
    int nr = vars.size()-1;
    _s_229.push_back(_s_191);
    _s_233.push_back(nr);
    return nr;
}
bool _s_340::_s_357(_s_189 &_s_358) {
    if (_s_358._s_196()!=_s_184) return false;
    std::vector<std::pair<int,const _s_189*> > _s_471;
    _s_471.push_back(std::pair<int,const _s_189*>(0,&(_s_358._s_195()[0])));
    while (_s_471.size()!=0) {
        std::vector<std::pair<int,const _s_189*> > _s_473;
        for (uint i=0; i<_s_471.size(); i++) {
            std::pair<int,const _s_189*> _s_472 = _s_471[i];
            const _s_189 *_s_422 = _s_472.second;
            switch (_s_422->_s_196()) {
            case _s_178:
            case _s_179:
            case _s_186:
            case _s_180:
                for (uint j=0; j<_s_422->_s_195().size(); j++) {
                    _s_473.push_back(std::pair<int,const _s_189*>(_s_472.first,&(_s_422->_s_195()[j])));
                }
                break;
            case _s_181:
                if (_s_472.first>=10) return false;
                _s_473.push_back(std::pair<int,const _s_189*>(_s_472.first+1,&(_s_422->_s_195()[0])));
                break;
            case _s_185:
            case _s_183:
            case _s_184:
                return false;
            case _s_187:
            case _s_188:
            case _s_182:
                break;
            default:
                throw "Not found.";
            }
        }
        _s_471 = _s_473;
    }
    std::cout << "Identified " << _s_358._s_26() << "as being limited safety!\n";
    return true;
}
void _s_340::_s_359(_s_189 &_s_358) {
    if (_s_358._s_196()!=_s_184) throw "Shouldn't happen: No G at the beginning of a safety spec node";
    ;
    _s_342.push_back(_s_358._s_195()[0]);
}
void _s_340::_s_243(bool _s_364) {
    uint _s_474[_s_342.size()];
    uint _s_475 = 0;
    for (uint i=0; i<_s_342.size(); i++) {
        std::cout << "Safety spec: " << _s_342[i]._s_26() << std::endl;
        std::set<_s_333> _s_476;
        _s_474[i] = _s_351(_s_342[i],0,_s_476);
        _s_475 = ((_s_475<_s_474[i])?(_s_474[i]):(_s_475));
        for (std::set<_s_333>::iterator it = _s_476.begin(); it != _s_476.end(); it++) {
            _s_345[it->_s_194()] = ((_s_345[it->_s_194()]<_s_474[i]-it->_s_337())?(_s_474[i]-it->_s_337()):(_s_345[it->_s_194()]));
        }
    }
    std::cout << "Maximum lookback (overall): " << _s_475 << std::endl;
    uint _s_477 = 0;
    {
        uint _s_478 = _s_475;
        while (_s_478 != 0) {
            _s_478 = _s_478 >> 1;
            _s_477++;
        }
    }
    std::cout << "Number of safety counter variables: " << _s_477 << std::endl;
    for (uint i=0; i<_s_477; i++) {
        std::ostringstream f;
        f << "SafetyStartingCounter" << i;
        _s_343.push_back(_s_284._s_247(f.str()));
        _s_344.push_back(_s_284._s_248(f.str()+"'"));
    }
    uint _s_479 = _s_284._s_247("BoundedSafetyUnsafePre");
    uint _s_480 = _s_284._s_248("BoundedSafetyUnsafePost");
    for (std::map<std::string, uint>::iterator it = _s_345.begin(); it != _s_345.end(); it++) {
        std::cout << "Maximum delay for var: " << it->first << "(" << it->second << ")" << std::endl;
        for (uint i=1; i<=it->second; i++) {
            std::ostringstream _s_191;
            _s_191 << "Safety_" << it->first << "_" << i;
            uint _s_481 = _s_284._s_247(_s_191.str());
            uint _s_482 = _s_284._s_248(_s_191.str()+"'");
            std::pair<uint,uint> _s_483(_s_481,_s_482);
            _s_346[std::pair<uint,std::string>(i,it->first)] = _s_483;
            std::cout << "Allocated variable: " << _s_191.str() << ": " << _s_483.first << "," << _s_483.second << std::endl;
        }
        _s_346[std::pair<uint,std::string>(0,it->first)] = std::pair<uint,uint>(_s_284._s_281()._s_246(it->first),65536);
    }
    _s_347 = mgr._s_33();
    for (uint i=0; i<_s_342.size(); i++) {
        std::cout << "Compiling Spec: " << i<< std::endl;
        _s_27 _s_486 = _s_353(_s_342[i],0,_s_474[i],"/tmp/spec");
        int _s_485 = _s_474[i];
        _s_27 _s_484 = mgr._s_33();
        for (uint i=0; i<_s_477; i++) {
            if ((_s_485 & 1)==0) {
                _s_484 |= _s_284._s_87(_s_343[i]);
            } else {
                _s_484 &= _s_284._s_87(_s_343[i]);
            }
            _s_485 = _s_485 >> 1;
        }
        _s_347 &= (!_s_484) | _s_486;
    }
    std::cout << "Compiling Specs done." << std::endl;
    if (_s_364) {
        _s_347 = (!_s_284._s_87(_s_480)) | _s_284._s_87(_s_479) | (!_s_347);
    } else {
        _s_347 |= _s_284._s_87(_s_480);
        _s_347 &= !_s_284._s_87(_s_479) | _s_284._s_87(_s_480);
    }
    _s_350 = mgr._s_33();
    for (uint i=0; i<_s_477; i++) {
        _s_350 &= !_s_284._s_87(_s_343[i]);
    }
    std::cout << "Initial stated obtained." << std::endl;
    _s_349 = !_s_284._s_87(_s_479);
    _s_348 = mgr._s_33();
    for (std::map<std::string, uint>::iterator it = _s_345.begin(); it != _s_345.end(); it++) {
        for (uint i=1; i<=it->second; i++) {
            std::pair<uint,uint> _s_52 = _s_346[std::pair<uint,std::string>(i-1,it->first)];
            std::pair<uint,uint> to = _s_346[std::pair<uint,std::string>(i,it->first)];
            _s_27 _s_487 = _s_284._s_87(_s_52.first);
            _s_27 _s_488 = _s_284._s_87(to.second);
            _s_348 &= ((!_s_487) & (!_s_488)) | (_s_487 & _s_488);
        }
    }
    std::cout << "History tracker written." << std::endl;
    _s_27 _s_489 = mgr._s_34();
    if (_s_475>0) {
        for (uint _s_490=0; _s_490<_s_475-1; _s_490++) {
            _s_27 _s_491 = mgr._s_33();
            int s = _s_490;
            for (uint i=0; i<_s_477; i++) {
                if ((s & 1)==0) {
                    _s_491 &= !_s_284._s_87(_s_343[i]);
                } else {
                    _s_491 &= _s_284._s_87(_s_343[i]);
                }
                s = s >> 1;
            }
            s = _s_490+1;
            for (uint i=0; i<_s_477; i++) {
                if ((s & 1)==0) {
                    _s_491 &= !_s_284._s_87(_s_344[i]);
                } else {
                    _s_491 &= _s_284._s_87(_s_344[i]);
                }
                s = s >> 1;
            }
            _s_489 |= _s_491;
        }
        {
            _s_27 _s_492 = mgr._s_33();
            _s_27 _s_493 = mgr._s_33();
            int s = _s_475-1;
            for (uint i=0; i<_s_477; i++) {
                if ((s & 1)==0) {
                    _s_492 |= _s_284._s_87(_s_343[i]);
                } else {
                    _s_492 &= _s_284._s_87(_s_343[i]);
                }
                s = s >> 1;
            }
            s = _s_475;
            for (uint i=0; i<_s_477; i++) {
                if ((s & 1)==0) {
                    _s_493 |= _s_284._s_87(_s_344[i]);
                } else {
                    _s_493 &= _s_284._s_87(_s_344[i]);
                }
                s = s >> 1;
            }
            _s_489 |= _s_492 & _s_493;
        }
        _s_348 &= _s_489;
    }
    std::cout << "SafetyCompiler has finished!" << std::endl;
}
_s_225::_s_225(BFBddManager &_s_53, _s_227 &_s_273, const std::vector<_s_27> _s_274, const std::vector<std::string> _s_275, const std::vector<uint> _s_276, const std::vector<uint> _s_277, const std::vector<uint> _s_278, const std::vector<uint> _s_279, _s_226 &_s_280) : mgr(_s_53), _s_257(_s_273), vars(_s_274), _s_229(_s_275), _s_258(_s_276), _s_259(_s_277), _s_260(_s_278), _s_261(_s_279), _s_234(_s_280), _s_268(NULL), _s_269(NULL) {
    _s_264 = mgr._s_33();
    for (uint i=0; i<_s_258.size(); i++) _s_264 &= !vars[_s_258[i]];
    _s_234._s_309(this);
    for (uint i=0; i<_s_258.size(); i++) {
        std::cout << "preOther" << ": " << _s_229[_s_258[i]] << "(" << _s_258[i] << ")" << std::endl;
    };
    for (uint i=0; i<_s_259.size(); i++) {
        std::cout << "postOther" << ": " << _s_229[_s_259[i]] << "(" << _s_259[i] << ")" << std::endl;
    };
    for (uint i=0; i<_s_260.size(); i++) {
        std::cout << "globalInput" << ": " << _s_229[_s_260[i]] << "(" << _s_260[i] << ")" << std::endl;
    };
    for (uint i=0; i<_s_261.size(); i++) {
        std::cout << "globalOutput" << ": " << _s_229[_s_261[i]] << "(" << _s_261[i] << ")" << std::endl;
    };
}
void _s_225::_s_270() {
    if (_s_268!=NULL) delete _s_268;
    if (_s_269!=NULL) delete _s_269;
    {
        static const std::vector<uint> _s_225::*cube[] = {
            &_s_225::_s_259,
            0
        };
        _s_265 = _s_36(cube);
    }
    {
        static const std::vector<uint> _s_225::*cube[] = {
            &_s_225::_s_260,
            0
        };
        _s_266 = _s_36(cube);
    }
    {
        static const std::vector<uint> _s_225::*cube[] = {
            &_s_225::_s_261,
            0
        };
        _s_267 = _s_36(cube);
    }
    _s_268 = new _s_29(_s_272(_s_258));
    _s_269 = new _s_29(_s_272(_s_259));
}
_s_225::~_s_225() {
    if (_s_268!=NULL) delete _s_268;
    if (_s_269!=NULL) delete _s_269;
}
_s_29 _s_225::_s_272 ( const std::vector<uint> &_s_52) const {
    std::vector<_s_27> _s_494;
    for ( uint i=0; i<_s_52.size(); i++ )
        _s_494.push_back(vars[_s_52[i]]);
    return mgr._s_38(_s_494);
}
uint _s_225::_s_247(const std::string &_s_191) {
    assert(vars.size()==_s_229.size());
    vars.push_back(mgr._s_35());
    int nr = vars.size()-1;
    _s_229.push_back(_s_191);
    _s_258.push_back(nr);
    return nr;
}
uint _s_225::_s_248(const std::string &_s_191) {
    assert(vars.size()==_s_229.size());
    vars.push_back(mgr._s_35());
    int nr = vars.size()-1;
    _s_229.push_back(_s_191);
    _s_259.push_back(nr);
    return nr;
}
_s_28 _s_225::_s_36(const std::vector<uint> _s_225::*list[]) {
    uint _s_466 = 0;
    for (uint nr = 0; list[nr]!=0; nr++) {
        _s_466 += (this->*list[nr]).size();
    }
    _s_27 *cube = new _s_27[_s_466];
    int *_s_37 = new int[_s_466];
    uint ptr = 0;
    for (uint nr = 0; list[nr]!=0; nr++) {
        for (uint i=0; i<(this->*list[nr]).size(); i++) {
            cube[ptr] = vars[(this->*list[nr])[i]];
            _s_37[ptr++] = 1;
        }
    }
    _s_28 _s_59 = mgr._s_36(cube,_s_37,_s_466);
    delete[] _s_37;
    delete[] cube;
    return _s_59;
}
void _s_225::_s_243() {
    _s_270();
    _s_27 _s_495 = mgr._s_33();
    _s_27 _s_496 = mgr._s_34();
    int _s_497 = 0;
    while (true) {
        _s_497++;
        std::cout << "Counter increase...";
        std::cout.flush();
        _s_234._s_311(_s_497+1);
        std::cout << "almost...";
        std::cout.flush();
        _s_270();
        _s_495 = _s_496;
        std::vector<_s_27> _s_314;
        _s_234._s_313(_s_314);
        std::cout << "done!\n";
        std::cout.flush();
        _s_27 _s_572 = mgr._s_34();
        _s_27 _s_573 = mgr._s_33();
        int _s_498=0;
        while (_s_572!=_s_573) {
            _s_572 = _s_573;
            _s_498++;
            _s_27 _s_500 = _s_573._s_69(*_s_269,*_s_268);
            for (uint i=0; i<_s_314.size(); i++) {
                _s_500 &= _s_314[i];
            }
            _s_27 _s_499 = _s_234._s_315();
            _s_500 = _s_500._s_71(_s_265);
            if (_s_281()._s_236()._s_218()) {
                _s_500 = _s_500._s_73(_s_267);
                _s_500 = _s_500._s_71(_s_266);
                _s_573 = _s_500 & (!_s_499);
            } else {
                _s_500 = _s_500._s_71(_s_267);
                _s_500 = _s_500._s_73(_s_266);
                _s_573 = _s_500 | _s_499;
            }
        }
        _s_496 = _s_573;
        _s_27 _s_501 = _s_264 & _s_234._s_316();
        if ((_s_496 & _s_501)!=mgr._s_34()) {
            if (_s_257._s_236()._s_218()) {
                std::cout << "Result: Specification is unrealisable!" << std::endl;
                if (_s_257._s_236()._s_567()) {
                    _s_575 s(*this,true);
                    s._s_243(_s_258,_s_259,_s_260,_s_261,_s_314,_s_501,_s_496);
                }
            } else {
                std::cout << "Result: Specification is realisable!" << std::endl;
                switch (_s_257._s_236()._s_221()) {
                case _s_176:
                    break;
                case _s_174:
                {
                    _s_321 _s_502(*this,_s_258,_s_259,_s_260,_s_261,_s_314);
                    _s_502._s_243(_s_501,_s_496);
                }
                break;
                case _s_175:
                {
                    _s_332 _s_502(*this,_s_258,_s_259,_s_260,_s_261,_s_314);
                    _s_502._s_243(_s_501,_s_496);
                }
                break;
                case _s_601:
                {
                    _s_598 _s_502(*this,_s_258,_s_259,_s_260,_s_261,_s_314,_s_257._s_236()._s_600());
                    _s_502._s_243(_s_501,_s_496);
                }
                break;
                default:
                    std::cerr << "Error: Invalid implementation extractor mode.\n";
                    throw "Fatal error:";
                };
                if (_s_257._s_236()._s_567()) {
                    _s_575 s(*this,false);
                    s._s_243(_s_258,_s_259,_s_260,_s_261,_s_314,_s_501,_s_496);
                }
            }
            return;
        }
    }
    std::cout << "I'm here: " << 6699 << std::endl;
}
void _s_225::_s_85(std::vector<std::string> &_s_92) const {
    _s_92.push_back("preOther");
    _s_92.push_back("postOther");
    _s_92.push_back("globalInput");
    _s_92.push_back("globalOutput");
}
void _s_225::_s_86(std::string type, std::vector<uint> &_s_93) const {
    if (type=="preOther") {
        for (uint i=0; i<_s_258.size(); i++) _s_93.push_back(_s_258[i]);
        return;
    };
    if (type=="postOther") {
        for (uint i=0; i<_s_259.size(); i++) _s_93.push_back(_s_259[i]);
        return;
    };
    if (type=="globalInput") {
        for (uint i=0; i<_s_260.size(); i++) _s_93.push_back(_s_260[i]);
        return;
    };
    if (type=="globalOutput") {
        for (uint i=0; i<_s_261.size(); i++) _s_93.push_back(_s_261[i]);
        return;
    };
    throw _s_2("VarType not found in SynthesisModule::getVariableTypes: " + type);
}
_s_227::_s_227(const _s_199 &_s_256) : _s_228(_s_256), mgr(1.1), _s_234(NULL), _s_235(NULL) {
    mgr._s_42(true);
    {
        std::vector<std::string> _s_503;
        _s_228._s_212(_s_503);
        for (uint i=0; i<_s_503.size(); i++) {
            vars.push_back(mgr._s_35());
            _s_230.push_back(vars.size()-1);
            _s_229.push_back("in_"+_s_503[i]);
        }
    }
    {
        std::vector<std::string> _s_504;
        _s_228._s_213(_s_504);
        for (uint i=0; i<_s_504.size(); i++) {
            vars.push_back(mgr._s_35());
            _s_231.push_back(vars.size()-1);
            _s_229.push_back("out_"+_s_504[i]);
        }
    }
    std::cout << "Variables for the synthesis procedure:" << std::endl;
    for (uint i=0; i<_s_229.size(); i++) {
        std::cout << "~> " << _s_229[i] << std::endl;
    }
}
uint _s_227::_s_246(const std::string &_s_191) {
    for (uint i=0; i<_s_229.size(); i++) {
        const std::string &_s_422 = _s_229[i];
        if (_s_422==("out_"+_s_191)) return i;
        if (_s_422==("in_"+_s_191)) return i;
    }
    throw _s_2("Variable occurring in specification cannot be found. It is neither defined to be output nor input: " + _s_191);
}
void _s_227::_s_243() {
    _s_234 = new _s_226(*this);
    _s_244();
}
void _s_227::_s_244() {
    std::vector<uint> _s_258;
    std::vector<uint> _s_259;
    _s_258 = _s_232;
    _s_259 = _s_233;
    std::vector< std::vector<uint> > _s_505;
    std::vector< std::vector<uint> > _s_506;
    _s_235 = new _s_225(mgr,*this,vars,_s_229,_s_258,_s_259,_s_230,_s_231,*_s_234);
    _s_235->_s_243();
}
void _s_227::_s_85(std::vector<std::string> &_s_92) const {
    _s_92.push_back("GlobalInput");
    _s_92.push_back("GlobalOutput");
    _s_92.push_back("SafetyPre");
    _s_92.push_back("SafetyPost");
}
void _s_227::_s_86(std::string type, std::vector<uint> &_s_93) const {
    if (type=="GlobalInput") {
        for (uint i=0; i<_s_230.size(); i++) _s_93.push_back(_s_230[i]);
        return;
    };
    if (type=="GlobalOutput") {
        for (uint i=0; i<_s_231.size(); i++) _s_93.push_back(_s_231[i]);
        return;
    };
    if (type=="SafetyPre") {
        for (uint i=0; i<_s_232.size(); i++) _s_93.push_back(_s_232[i]);
        return;
    };
    if (type=="SafetyPost") {
        for (uint i=0; i<_s_233.size(); i++) _s_93.push_back(_s_233[i]);
        return;
    };
    throw _s_2("VarType not found in SynthesisModule::getVariableTypes: " + type);
}
_s_227::~_s_227() {
    if (_s_235!=NULL) delete _s_235;
    if (_s_234!=NULL) delete _s_234;
}
char _s_575::_s_578() {
    struct termios _s_586, _s_587;
    tcgetattr(0, &_s_586);
    tcgetattr(0, &_s_587);
    _s_587.c_lflag &= ~ICANON;
    _s_587.c_lflag &= ~ECHO;
    _s_587.c_cc[VMIN] = 1;
    _s_587.c_cc[VTIME] = 0;
    tcsetattr(0, TCSANOW, &_s_587);
    char _s_588 = getchar();
    tcsetattr(0, TCSANOW, &_s_586);
    return _s_588;
}
_s_27 _s_575::_s_579(_s_27 _s_580, std::vector<_s_27> &vars, bool _s_581) {
    assert(_s_580!=_s_580._s_60()->_s_34());
    for (uint i=0; i<vars.size(); i++) {
        _s_27 _s_584 = _s_580 & vars[i];
        _s_27 _s_585 = _s_580 & (!vars[i]);
        if (_s_584==_s_584._s_60()->_s_34()) {
            _s_580 = _s_585;
            assert(_s_580!=_s_585._s_60()->_s_34());
        } else if (_s_585==_s_585._s_60()->_s_34()) {
            _s_580 = _s_584;
            assert(_s_580!=_s_584._s_60()->_s_34());
        } else {
            if (_s_581) {
                if ((rand() % 2)==0) _s_580=_s_584;
                else _s_580 = _s_585;
            } else {
                _s_580 = _s_585;
            }
        }
    }
    return _s_580;
}
_s_27 _s_575::_s_582(BFBddManager &mgr, std::vector<_s_27> &vars) {
    _s_27 _s_59 = mgr._s_33();
    for (uint i=0; i<vars.size(); i++) {
        char c;
        do {
            c = _s_578();
        } while ((c!='0') && (c!='1'));
        std::cout << c;
        if (c=='0')
            _s_59 &= (!vars[i]);
        else
            _s_59 &= vars[i];
    }
    return _s_59;
}
void _s_575::_s_583(_s_27 _s_580, std::vector<_s_27> &vars) {
    for (uint i=0; i<vars.size(); i++) {
        _s_27 _s_584 = _s_580 & vars[i];
        _s_27 _s_585 = _s_580 & (!vars[i]);
        if (_s_584==_s_584._s_60()->_s_34()) {
            std::cout << "0";
        } else if (_s_585==_s_585._s_60()->_s_34()) {
            std::cout << "1";
        } else {
            throw "Ambigous state given";
        }
    }
}
void _s_575::_s_243(const std::vector<uint> &_s_322, const std::vector<uint> &_s_323, const std::vector<uint> &_s_230, const std::vector<uint> &_s_231, const std::vector<_s_27> &_s_314, _s_27 _s_330, const _s_27 &_s_331)
{
    BFBddManager &mgr = _s_576._s_282();
    std::vector<_s_27> _s_401;
    std::vector<_s_27> _s_402;
    std::vector<_s_27> _s_403;
    std::vector<_s_27> _s_404;
    std::vector<_s_27> _s_405;
    std::vector<_s_27> _s_406;
    std::vector<_s_27> _s_407;
    for (std::vector<uint>::const_iterator it = _s_322.begin(); it!=_s_322.end(); it++) {
        _s_401.push_back(_s_576._s_87(*it));
        _s_405.push_back(_s_401.back());
    }
    for (std::vector<uint>::const_iterator it = _s_323.begin(); it!=_s_323.end(); it++) {
        _s_402.push_back(_s_576._s_87(*it));
        _s_406.push_back(_s_576._s_87(*it));
        _s_407.push_back(_s_576._s_87(*it));
    }
    for (std::vector<uint>::const_iterator it = _s_230.begin(); it!=_s_230.end(); it++) {
        _s_403.push_back(_s_576._s_87(*it));
        _s_405.push_back(_s_576._s_87(*it));
        _s_406.push_back(_s_576._s_87(*it));
    }
    for (std::vector<uint>::const_iterator it = _s_231.begin(); it!=_s_231.end(); it++) {
        _s_404.push_back(_s_576._s_87(*it));
        _s_405.push_back(_s_576._s_87(*it));
        _s_406.push_back(_s_576._s_87(*it));
        _s_407.push_back(_s_576._s_87(*it));
    }
    _s_29 _s_408 = mgr._s_38(_s_402);
    _s_28 _s_409 = mgr._s_36(_s_402);
    _s_29 _s_410 = mgr._s_38(_s_401);
    _s_28 _s_411 = mgr._s_36(_s_401);
    _s_28 _s_412 = mgr._s_36(_s_403);
    _s_28 _s_413 = mgr._s_36(_s_404);
    _s_28 _s_414 = mgr._s_36(_s_405);
    _s_28 _s_415 = mgr._s_36(_s_406);
    _s_28 _s_416 = mgr._s_36(_s_407);
    std::cout << "\n\n\n------------------------------------\n";
    std::cout << "Running simulator: You play the " << (_s_577?"system":"environment") << ", I play the " << (_s_577?"environment":"system") << "\n";
    bool _s_589 = false;
    std::cout << "Do you want the game position to be printed? (y/n)" << std::endl;
    {
        char c = 'k';
        while ((c!='y') && (c!='Y') && (c!='n') && (c!='N')) c = _s_578();
        _s_589 = ((c=='y') || (c=='Y'));
        std::cout << c;
    }
    std::cout << "\n\nOk. This simulator can be used as follows: At each step of the computation, you have the possibility to:\n - Reset the simulation to the initial state (by pressing r)\n - Go back to a previous state (by pressing g)\n - Abort the simulator (by pressing x)\n - Continue, but let the computer act as randomly as possible (by pressing z)\n - Continue with the simulation (by pressing c)\nHere we go!\n\n";
    {
        std::vector<std::string> _s_590;
        _s_590.push_back("");
        _s_590.push_back("Command");
        _s_590.push_back("");
        _s_590.push_back("State number (during the simulation run)");
        _s_590.push_back(" ");
        _s_590.push_back(" ");
        _s_590.push_back(" ");
        _s_590.push_back(" ");
        _s_590.push_back(" ");
        _s_590.push_back("");
        if (_s_589) {
            for (uint i=0; i<_s_322.size(); i++) {
                _s_590.push_back(_s_576._s_88(_s_322[i]));
            }
            _s_590.push_back("");
        }
        for (uint i=0; i<_s_230.size(); i++) {
            _s_590.push_back(_s_576._s_88(_s_230[i]));
        }
        _s_590.push_back("");
        for (uint i=0; i<_s_231.size(); i++) {
            _s_590.push_back(_s_576._s_88(_s_231[i]));
        }
        _s_590.push_back("");
        size_t _s_591 = 0;
        for (std::vector<std::string>::iterator it = _s_590.begin(); it!=_s_590.end(); it++) {
            _s_591 = std::max(_s_591,it->length());
        }
        for (uint i=0; i<_s_590.size(); i++) {
            if (_s_590[i].length()==0) {
                for (uint j=0; j<i; j++) {
                    if ((_s_590[j].length()==0) && (j<i))
                        std::cout << "|";
                    else
                        std::cout << " ";
                }
                std::cout << "+";
                for (uint j=i+1; j<_s_590.size()+_s_591+3; j++) std::cout << "-";
                std::cout << "\n";
            } else {
                for (uint j=0; j<_s_590.size(); j++) {
                    if ((_s_590[j].length()==0) && (j<i))
                        std::cout << "|";
                    else
                        std::cout << " ";
                }
                std::cout << " " << _s_590[i] << "\n";
            }
        }
        for (uint i=0; i<_s_590.size(); i++) {
            if (_s_590[i].length()==0) {
                std::cout << "+";
            } else {
                std::cout << "-";
            }
        }
        std::cout << "\n";
    }
    std::vector<_s_27> _s_592;
    do {
        char _s_593;
        bool _s_594 = false;
        std::cout << "|";
        std::cout.flush();
        if (_s_592.size()==0) {
            _s_593 = 'r';
        } else {
            do {
                _s_593 = _s_578();
            } while ((_s_593!='c') && (_s_593!='x') && (_s_593!='g') && (_s_593!='z') && (_s_593!='r'));
        }
        std::cout << _s_593;
        std::cout << "|";
        if (_s_593=='x') {
            std::cout << "\n\n";
            return;
        }
        if (_s_593=='r') {
            _s_27 _s_536 = _s_579(_s_330 & _s_331,_s_401,false);
            _s_592.clear();
            _s_592.push_back(_s_536);
        }
        if (_s_593=='z') {
            _s_594 = true;
        }
        if (_s_593=='g') {
            int _s_595 = -1;
            std::string _s_588;
            do {
                std::cin >> _s_588;
                std::istringstream is(_s_588);
                is >> _s_595;
                if (is.fail()) {
                    _s_595 = -1;
                    std::cout << "|g| -> Illegal step number\n";
                } else if (_s_595 < 0) {
                    _s_595 = -1;
                    std::cout << "|g| -> Step number must be positive \n";
                } else if (_s_595 >= (int)(_s_592.size())-1) {
                    _s_595 = -1;
                    std::cout << "|g| -> Step number must not be too large - i.e., smaller or equal to " << _s_592.size()-2 << "\n";
                }
                if (_s_595 == -1) {
                    std::cout << "|g|";
                }
            } while (_s_595==-1);
            std::cout << "|g|";
            while ((_s_592.size()>(uint)(_s_595)+1) && (_s_592.size()>0)) {
                _s_592.pop_back();
            }
        }
        std::ostringstream _s_596;
        _s_596 << _s_592.size()-1;
        for (uint j=0; j<6-_s_596.str().length(); j++) {
            std::cout << " ";
        }
        std::cout << _s_596.str();
        std::cout << "|";
        if (_s_589) {
            _s_583(_s_592.back(),_s_401);
            std::cout << "|";
        }
        _s_27 _s_597;
        if (!_s_577) {
            _s_597 = _s_582(mgr,_s_403);
            _s_597 &= _s_592.back();
            for (uint i=0; i<_s_314.size(); i++) {
                _s_597 &= _s_314[i];
            }
            _s_597 &= _s_331._s_69(_s_410,_s_408);
            if (_s_597==mgr._s_34()) {
                std::cout << "| Some safety assumption has already been violated. ";
            } else {
                _s_597 = _s_579(_s_597,_s_404,_s_594);
                _s_597 = _s_579(_s_597,_s_402,_s_594);
                std::cout << "|";
                _s_583(_s_597,_s_404);
                _s_597 = _s_597._s_71(_s_414)._s_69(_s_408,_s_410);
            }
            _s_592.push_back(_s_597);
        } else {
            _s_27 _s_597 = _s_592.back();
            for (uint i=0; i<_s_314.size(); i++) {
                _s_597 &= _s_314[i];
            }
            _s_597 &= _s_331._s_69(_s_410,_s_408);
            if (_s_597==mgr._s_34()) {
                std::cout << "| Some safety guarantee has already been violated. ";
            } else {
                _s_597 &= _s_579(_s_597._s_71(_s_409)._s_73(_s_413),_s_403,_s_594);
                _s_583(_s_597,_s_403);
                std::cout << "|";
                _s_597 &= _s_582(mgr,_s_404);
                if (_s_597==mgr._s_34()) {
                    std::cout << "| Some safety guarantee has already been violated. ";
                } else {
                    _s_597 = _s_579(_s_597,_s_402,_s_594);
                    _s_597 = _s_597._s_71(_s_414)._s_69(_s_408,_s_410);
                }
            }
            _s_592.push_back(_s_597);
        }
        std::cout << "|\n";
    } while(true);
}
uint _s_101::_s_112() const {
    assert(_s_108.size()==_s_109.size());
    return(_s_108.size());
}
uint _s_101::_s_113(const std::string &name, bool _s_507) {
    uint nr = _s_108.size();
    _s_108.push_back(name);
    _s_109.push_back(_s_507);
    return nr;
}
void _s_101::_s_114(uint _s_52, uint to, _s_27 _s_116) {
    std::map<std::pair<uint,uint>,_s_27 >::iterator it = _s_110.find(std::pair<uint,uint>(_s_52,to));
    if (it != _s_110.end()) {
        it->second = it->second | _s_116;
    } else {
        _s_110[std::pair<uint,uint>(_s_52,to)] = _s_116;
    }
}
void _s_101::_s_114(uint _s_52, std::string to, _s_27 _s_116) {
    _s_114(_s_52,_s_117(to),_s_116);
}
uint _s_101::_s_117(const std::string &to) const {
    for (uint i=0; i<_s_108.size(); i++) {
        if (_s_108[i]==to) return i;
    }
    throw _s_2("State name not found: "+to);
}
std::string _s_101::_s_118(uint _s_94) const {
    return _s_108[_s_94];
}
bool _s_101::_s_119(uint _s_120) const {
    return _s_109[_s_120];
}
void _s_101::_s_134(_s_101 &_s_126) const {
    assert(&_s_126!=this);
    _s_126._s_108 = _s_108;
    _s_126._s_109.clear();
    _s_126._s_110.clear();
    for (uint i=0; i<_s_108.size(); i++) {
        _s_126._s_109.push_back(_s_131(_s_109[i]));
    }
    uint _s_508 = _s_108.size();
    _s_126._s_108.push_back("Absorber");
    _s_126._s_109.push_back(true);
    for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        uint to = it->first.second;
        if (!(_s_109[to])) {
            _s_126._s_110[it->first] = it->second;
        }
    }
    _s_126._s_110[std::pair<uint,uint>(_s_508,_s_508)] = _s_111->_s_98()._s_33();
    for (uint _s_165=0; _s_165<_s_108.size(); _s_165++) {
        _s_27 _s_443 = _s_111->_s_98()._s_34();
        for (uint i=0; i<_s_108.size(); i++) {
            _s_443 |= _s_139(_s_165,i);
        }
        if (_s_443!=_s_111->_s_98()._s_33()) {
            _s_126._s_110[std::pair<uint,uint>(_s_165,_s_508)] = !_s_443;
        }
    }
    _s_126._s_121();
}
void _s_101::_s_133() {
    std::set<int> _s_417;
    _s_417.insert(0);
    uint _s_509 = 0;
    for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        if ((it->first.first==0) && (it->second != _s_111->_s_98()._s_34())) _s_417.insert(it->first.second);
    }
    while (_s_417.size()!=_s_509) {
        _s_509 = _s_417.size();
        for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
            if ((_s_417.count(it->first.first)>0) && (it->second != _s_111->_s_98()._s_34())) _s_417.insert(it->first.second);
        }
    }
    for (uint i=0; i<_s_108.size(); i++) {
        if (_s_417.count(i)<1) {
            _s_109[i] = false;
        }
    }
}
void _s_101::_s_121() {
    std::set<int> _s_417;
    _s_417.insert(0);
    uint _s_509 = 0;
    for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        if ((it->first.first==0) && (it->second != _s_111->_s_98()._s_34())) _s_417.insert(it->first.second);
    }
    while (_s_417.size()!=_s_509) {
        _s_509 = _s_417.size();
        for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
            if ((_s_417.count(it->first.first)>0) && (it->second != _s_111->_s_98()._s_34())) _s_417.insert(it->first.second);
        }
    }
    std::vector<std::string> _s_510;
    std::vector<bool> _s_511;
    std::map<std::pair<uint,uint>,_s_27 > _s_512;
    int map[_s_108.size()];
    int pointer = 0;
    for (uint i=0; i<_s_108.size(); i++) {
        if (_s_417.count(i)>0) {
            map[i] = pointer++;
        } else {
            map[i] = -1;
        }
    }
    for (uint i=0; i<_s_108.size(); i++) {
        if (map[i]!=-1) {
            _s_510.push_back(_s_108[map[i]]);
            _s_511.push_back(_s_109[map[i]]);
        }
    }
    for (std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        if ((map[it->first.first]>=0) && (map[it->first.second]>=0)) {
            _s_512[std::pair<uint,uint>(map[it->first.first],map[it->first.second])] = it->second;
        }
    }
    _s_108 = _s_510;
    _s_109 = _s_511;
    _s_110 = _s_512;
}
void _s_101::_s_143(_s_101 &_s_144, std::string _s_145) {
    {
        std::istringstream in(_s_145);
        try {
            _s_20(in,"never {");
            bool _s_513 = false;
            {
                std::string _s_515;
                std::getline((in),_s_515);
            }
            while (!_s_513) {
                char _s_514[256];
                in.get(_s_514,256,':');
                std::string _s_456 = _s_514;
                boost::trim(_s_456);
                if (_s_456!="}") {
                    _s_20(in,":");
                    bool _s_516 = (_s_456.substr(0,6)=="accept");
                    _s_144._s_113(_s_456,_s_516);
                    std::string _s_515;
                    std::getline(in,_s_515);
                    boost::trim(_s_515);
                    while ((!(_s_515=="fi;")) && (!(_s_515=="skip")) && (!(_s_515=="false;"))) {
                        if (in.fail()) throw _s_2("Premature termination of automaton description");
                        std::getline(in,_s_515);
                        boost::trim(_s_515);
                    }
                } else {
                    _s_513 = true;
                }
            }
        } catch (_s_18 e) {
            throw _s_2("LTL compilation error. Reason: "+e._s_19());
        }
    }
    {
        std::istringstream in(_s_145);
        try {
            _s_20(in,"never {");
            bool _s_513 = false;
            {
                std::string _s_515;
                std::getline((in),_s_515);
            };
            int _s_520 = 0;
            while (!_s_513) {
                char _s_514[256];
                in.get(_s_514,256,':');
                std::string _s_456 = _s_514;
                boost::trim(_s_456);
                if (_s_456!="}") {
                    _s_20(in,":");
                    {
                        std::string _s_515;
                        std::getline((in),_s_515);
                    };
                    std::string _s_521;
                    std::getline(in,_s_521);
                    boost::trim(_s_521);
                    if (_s_521=="if") {
                        std::string _s_517;
                        std::getline(in,_s_517);
                        boost::trim(_s_517);
                        while (_s_517!="fi;") {
                            if (_s_517.substr(0,3)!=":: ")
                                throw _s_2("LTL compilation error: Illegal transition line:"+_s_517);
                            size_t _s_518 = _s_517.find(" -> goto ",0);
                            if (_s_518 == std::string::npos) {
                                throw _s_2("LTL compilation error: Cannot find '-> goto' in transition: "+_s_517);
                            }
                            std::string _s_519 = _s_517.substr(0,_s_518).substr(3);
                            std::string to = _s_517.substr(_s_518+9);
                            boost::trim(to);
                            _s_144._s_114(_s_520,to,_s_144._s_141(_s_519));
                            std::getline(in,_s_517);
                            boost::trim(_s_517);
                        }
                    } else if (_s_521=="skip") {
                        _s_144._s_114(_s_520,_s_520,_s_144._s_111->_s_98()._s_33());
                    } else if (_s_521=="false;") {
                    } else {
                        throw _s_2("LTL compilation error: 'if' expected but "+_s_521+" found.");
                    }
                    _s_520++;
                } else {
                    _s_513 = true;
                }
            }
        } catch (_s_18 e) {
            throw _s_2("LTL compilation error. Reason: "+e._s_19());
        }
    }
}
_s_27 _s_101::_s_141(std::string _s_142) {
    int _s_522 = 0;
    bool _s_523 = false;
    for (uint i=0; i<_s_142.length(); i++) {
        switch (_s_142[i]) {
        case '(':
            _s_522 += 1;
            break;
        case ')':
            _s_522 -= 1;
            break;
        case '|':
            if (_s_522>0) _s_523 = true;
            break;
        }
        if (_s_522<0) throw "Error: Unmatched braces in LTLCompiler::convertSPINBoolFormulaToPrefixNotation";
    }
    if (_s_522!=0) throw "Error: Unmatched braces in LTLCompiler::convertSPINBoolFormulaToPrefixNotation";
    if (_s_523) {
        if ((_s_142[0]!='(') || (_s_142[_s_142.length()-1]!=')')) throw "Error: Too complex braces in LTLCompiler::convertSPINBoolFormulaToPrefixNotation";
        _s_142 = _s_142.substr(1,_s_142.length()-2);
    }
    _s_27 _s_59 = _s_111->_s_98()._s_34();
    std::vector<std::string> _s_524;
    boost::iter_split(_s_524, _s_142, boost::first_finder(" || "));
    for (uint i=0; i<_s_524.size(); i++) {
        std::string _s_422 = _s_524[i];
        if (_s_422[0]=='(') {
            assert(_s_422[_s_422.length()-1]==')');
            _s_422 = _s_422.substr(1,_s_422.length()-2);
        } else {
            assert(_s_422[_s_422.length()-1]!=')');
        }
        std::vector<std::string> _s_525;
        boost::iter_split(_s_525, _s_422, boost::first_finder(" && "));
        _s_27 andPart = _s_111->_s_98()._s_33();
        for (uint j=0; j<_s_525.size(); j++) {
            _s_422 = _s_525[j];
            bool _s_526 = _s_422[0]=='!';
            if (_s_526) _s_422 = _s_422.substr(1,std::string::npos);
            _s_27 value;
            if (_s_422=="1") {
                value = _s_111->_s_98()._s_33();
            } else {
                value = _s_111->_s_99(_s_422);
            }
            if (_s_526) {
                andPart &= !value;
            } else {
                andPart &= value;
            }
        }
        _s_59 |= andPart;
    }
    return _s_59;
}
_s_27 _s_101::_s_139(uint _s_52, uint to) const {
    std::map<std::pair<uint,uint>,_s_27 >::const_iterator it = _s_110.find(std::pair<uint,uint>(_s_52,to));
    if (it==_s_110.end()) return _s_111->_s_98()._s_34();
    return it->second;
}
bool _s_101::_s_124() const {
    for (uint i=0; i<_s_108.size(); i++) {
        if (_s_109[i] && (!_s_131(i))) return false;
    }
    return true;
}
bool _s_101::_s_131(uint _s_132) const {
    for (uint i=0; i<_s_108.size(); i++) {
        if (i!=_s_132) {
            std::map< std::pair<uint,uint>, _s_27>::const_iterator it = _s_110.find(std::pair<uint,uint>(_s_132,i));
            if (!((it==_s_110.end()) || (it->second == _s_111->_s_98()._s_34()))) return false;
        } else {
            std::map< std::pair<uint,uint>, _s_27>::const_iterator it = _s_110.find(std::pair<int,int>(_s_132,_s_132));
            if (it==_s_110.end()) return false;
            if (it->second != _s_111->_s_98()._s_33()) return false;
        }
    }
    return true;
}
bool _s_101::_s_123() const {
    for (uint i=0; i<_s_108.size(); i++) {
        for (uint j=0; j<_s_108.size(); j++) {
            if (_s_110.count(std::pair<uint,uint>(i,j))>0) {
                for (uint k=0; k<_s_108.size(); k++) {
                    if (k!=j) {
                        if (_s_110.count(std::pair<uint,uint>(i,k))>0) {
                            if ((_s_110.find(std::pair<uint,uint>(i,j))->second & _s_110.find(std::pair<uint,uint>(i,k))->second) != _s_111->_s_98()._s_34()) {
                                std::cout << "Non-det on " << _s_108[i] << "," << _s_108[j] << "," << _s_108[k] << "\n";
                                return false;
                            }
                        }
                    }
                }
            }
        }
    }
    return true;
}
void _s_101::_s_122(_s_101 &_s_59) const {
    if (!_s_124()) throw "Error: NBA::makeDeterministicIfAllAcceptingStatesAreAbsorbing() called but the automaton does not fulfill the requirements states in the name of this function.";
    _s_59 = _s_101(_s_111);
    std::map<std::set<uint>,uint> _s_527;
    std::map<std::set<uint>,uint> _s_528;
    std::set<uint> _s_529;
    _s_529.insert(0);
    _s_527[_s_529] = 0;
    _s_59._s_108.push_back("newInit");
    _s_59._s_109.push_back(_s_109[0]);
    if (_s_109[0]) {
        _s_59._s_110[std::pair<uint,uint>(0,0)] = _s_111->_s_98()._s_33();
        return;
    }
    uint _s_530 = 0;
    while (_s_527.size()>0) {
        std::set<uint> _s_422 = _s_527.begin()->first;
        uint _s_531 = _s_527.begin()->second;
        _s_527.erase(_s_527.begin());
        _s_528[_s_422] = _s_531;
        std::map<std::set<uint>,_s_27> _s_532;
        _s_532[std::set<uint>()] = _s_111->_s_98()._s_33();
        for (std::set<uint>::iterator it = _s_422.begin(); it!=_s_422.end(); it++) {
            uint _s_536 = *it;
            std::map<std::set<uint>,_s_27> _s_533 = _s_532;
            _s_532.clear();
            _s_532[std::set<uint>()] = _s_111->_s_98()._s_33();
            for (uint next=0; next<_s_108.size(); next++) {
                std::map<std::set<uint>,_s_27> _s_534;
                std::map<std::set<uint>,_s_27> _s_535;
                _s_27 _s_537 = _s_139(_s_536,next);
                for (std::map<std::set<uint>,_s_27>::iterator it2 = _s_532.begin(); it2!=_s_532.end(); it2++) {
                    if (it2->first.count(next)==0) {
                        _s_27 _s_538 = it2->second & _s_537;
                        if (_s_538!=_s_111->_s_98()._s_34()) _s_534[it2->first] = _s_538;
                        _s_538 = it2->second & (!_s_537);
                        if (_s_538!=_s_111->_s_98()._s_34()) _s_535[it2->first] =_s_538;
                    } else {
                        _s_534[it2->first] = it2->second;
                    }
                }
                _s_532 = _s_535;
                for (std::map<std::set<uint>,_s_27>::iterator it2 = _s_534.begin(); it2!=_s_534.end(); it2++) {
                    std::set<uint> _s_574 = it2->first;
                    _s_574.insert(next);
                    assert(_s_532.count(_s_574)==0);
                    _s_532[_s_574] = it2->second;
                }
            }
            std::map<std::set<uint>,_s_27> _s_539 = _s_532;
            _s_532.clear();
            for (std::map<std::set<uint>,_s_27>::iterator it2 = _s_533.begin(); it2!=_s_533.end(); it2++) {
                for (std::map<std::set<uint>,_s_27>::iterator it3 = _s_539.begin(); it3!=_s_539.end(); it3++) {
                    _s_27 _s_541 = it2->second & it3->second;
                    if (_s_541!=_s_111->_s_98()._s_34()) {
                        std::set<uint> _s_540;
                        _s_540.insert(it2->first.begin(),it2->first.end());
                        _s_540.insert(it3->first.begin(),it3->first.end());
                        if (_s_532.count(_s_540)==0) {
                            _s_532[_s_540] = _s_541;
                        } else {
                            _s_532[_s_540] |= _s_541;
                        }
                    }
                }
            }
        }
        for (std::map<std::set<uint>,_s_27>::iterator it = _s_532.begin(); it!=_s_532.end(); it++) {
            if (_s_527.count(it->first)>0) {
                assert(_s_59._s_109[_s_527[it->first]] || (_s_59._s_110.count(std::pair<uint,uint>(_s_531,_s_527[it->first]))==0));
                _s_59._s_110[std::pair<uint,uint>(_s_531,_s_527[it->first])] = it->second;
            } else if (_s_528.count(it->first)>0) {
                assert(_s_59._s_109[_s_528[it->first]] || (_s_59._s_110.count(std::pair<uint,uint>(_s_531,_s_528[it->first]))==0));
                _s_59._s_110[std::pair<uint,uint>(_s_531,_s_528[it->first])] = it->second;
            } else {
                std::ostringstream _s_542;
                bool first = true;
                bool _s_511 = false;
                for (std::set<uint>::iterator it2 = it->first.begin(); it2!=it->first.end(); it2++) {
                    if (first) {
                        first = false;
                    } else {
                        _s_542 << "_AND_";
                    }
                    _s_542 << _s_108[*it2];
                    _s_511 |= _s_131(*it2);
                }
                if (_s_511) {
                    if (_s_530==0) {
                        uint _s_543 = _s_59._s_108.size();
                        _s_530 = _s_543;
                        _s_59._s_108.push_back("absorb");
                        _s_59._s_109.push_back(_s_511);
                        _s_528[it->first] = _s_543;
                        _s_59._s_110[std::pair<uint,uint>(_s_543,_s_543)] = _s_111->_s_98()._s_33();
                        _s_59._s_110[std::pair<uint,uint>(_s_531,_s_543)] = it->second;
                    } else {
                        _s_59._s_110[std::pair<uint,uint>(_s_531,_s_530)] = it->second;
                        _s_528[it->first] = _s_530;
                    }
                } else {
                    uint _s_543 = _s_59._s_108.size();
                    _s_59._s_108.push_back(_s_542.str());
                    _s_59._s_109.push_back(_s_511);
                    assert(_s_527.count(it->first)==0);
                    _s_527[it->first] = _s_543;
                    _s_59._s_110[std::pair<uint,uint>(_s_531,_s_543)] = it->second;
                }
            }
            std::set<uint> _s_544;
            for (std::map<std::set<uint>,uint>::iterator it = _s_528.begin(); it!=_s_528.end(); it++) {
                assert(_s_59._s_109[it->second] || (_s_544.count(it->second)==0));
                _s_544.insert(it->second);
            }
            for (std::map<std::set<uint>,uint>::iterator it = _s_527.begin(); it!=_s_527.end(); it++) {
                assert(_s_59._s_109[it->second] || (_s_544.count(it->second)==0));
                _s_544.insert(it->second);
            }
        }
    }
    if (!_s_59._s_124()) throw "Error: NBA::makeDeterministicIfAllAcceptingStatesAreAbsorbing() failed to make a safety automaton";
}
void _s_101::_s_125(const _s_101 &_s_127, _s_101 &_s_126) const {
    assert(_s_111 == _s_127._s_111);
    _s_126 = _s_101(_s_111);
    int _s_545 = 1;
    int _s_546 = _s_108.size()+1;
    _s_126._s_108.push_back("ORinit");
    _s_126._s_109.push_back(false);
    for (std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        if (it->first.first ==0) {
            _s_126._s_110[std::pair<uint,uint>(0,it->first.second+_s_545)] = it->second;
        }
    }
    for (std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_127._s_110.begin(); it!=_s_127._s_110.end(); it++) {
        if (it->first.first ==0) {
            _s_126._s_110[std::pair<uint,uint>(0,it->first.second+_s_546)] = it->second;
        }
    }
    for (uint i=0; i<_s_108.size(); i++) {
        _s_126._s_108.push_back(_s_108.at(i));
        _s_126._s_109.push_back(_s_109.at(i));
    }
    for (std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_110.begin(); it!=_s_110.end(); it++) {
        _s_126._s_110[std::pair<uint,uint>(it->first.first+_s_545,it->first.second+_s_545)] = it->second;
    }
    for (uint i=0; i<_s_127._s_108.size(); i++) {
        _s_126._s_108.push_back(_s_127._s_108.at(i));
        _s_126._s_109.push_back(_s_127._s_109.at(i));
    }
    for (std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_127._s_110.begin(); it!=_s_127._s_110.end(); it++) {
        _s_126._s_110[std::pair<uint,uint>(it->first.first+_s_546,it->first.second+_s_546)] = it->second;
    }
}
std::string _s_101::_s_140() const {
    std::ostringstream _s_547;
    _s_547 << "never {\n";
    std::vector<std::string> _s_548;
    for (uint i=0; i<_s_108.size(); i++) {
        std::string u = "";
        if (_s_109[i]) {
            u = u + "accept_";
        } else {
            u = u + "notaccept_";
        }
        u = u + _s_108[i];
        if (i==0) {
            u = u + "_init";
        } else {
            u = u + "_trans";
        }
        _s_548.push_back(u);
    }
    std::vector<std::string> _s_549;
    _s_111->_s_100(_s_549);
    for (uint i=0; i<_s_108.size(); i++) {
        _s_547 << _s_548[i] << ":\n      if\n";
        for (uint j=0; j<_s_108.size(); j++) {
            _s_27 t = _s_139(i,j);
            if (t!=_s_111->_s_98()._s_34()) {
                _s_102(t,_s_549,0,"      :: (" , ") -> goto "+_s_548[j]+";\n", _s_547);
            }
        }
        _s_547 << "      fi;\n";
    }
    _s_547 << "}";
    return _s_547.str();
}
void _s_101::_s_130() {
    bool _s_426;
    do {
        _s_426 = true;
        for (uint i=0; i<_s_108.size(); i++) {
            if (_s_109[i]) {
                std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_110.find(std::pair<uint,uint>(i,i));
                if ((it==_s_110.end()) || (it->second==_s_111->_s_98()._s_34())) {
                    bool _s_550 = false;
                    for (uint j=0; j<_s_108.size(); j++) {
                        std::pair<uint,uint> next(i,j);
                        if (_s_110.count(next)>0) {
                            if (!_s_109[j]) {
                                if (_s_110.find(next)->second!=_s_111->_s_98()._s_34()) {
                                    _s_550 = true;
                                }
                            }
                        }
                    }
                    if (!_s_550) {
                        _s_426 = false;
                        _s_109[i] = false;
                    }
                }
            }
        }
    } while (!_s_426);
    for (uint i=0; i<_s_108.size(); i++) {
        if (_s_109[i]) {
            std::set<uint> _s_417;
            std::list<uint> _s_425;
            for (uint j=0; j<_s_108.size(); j++) {
                if (_s_139(i,j) != _s_111->_s_98()._s_34()) {
                    _s_417.insert(j);
                    _s_425.push_back(j);
                }
            }
            while (_s_425.size()>0) {
                uint _s_422 = *(_s_425.begin());
                _s_425.pop_front();
                for (uint j=0; j<_s_108.size(); j++) {
                    if (_s_417.count(j)==0) {
                        if (_s_139(_s_422,j) != _s_111->_s_98()._s_34()) {
                            _s_417.insert(j);
                            _s_425.push_back(j);
                        }
                    }
                }
            }
            if (_s_417.count(i)==0) _s_109[i] = false;
        }
    }
}
void _s_101::_s_128(std::set<uint> &_s_129) const {
    bool _s_426;
    _s_129.clear();
    bool _s_511[_s_108.size()];
    for (uint i=0; i<_s_108.size(); i++) {
        _s_511[i] = _s_109[i];
    }
    do {
        _s_426 = true;
        for (uint i=0; i<_s_108.size(); i++) {
            if (_s_511[i]) {
                std::pair<uint,uint> _s_551(i,i);
                std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_110.find(_s_551);
                if ((it!=_s_110.end()) && (it->second!=_s_111->_s_98()._s_34())) {
                    bool _s_550 = false;
                    for (uint j=0; j<_s_108.size(); j++) {
                        std::pair<uint,uint> next(i,j);
                        if (_s_110.count(next)>0) {
                            if (!_s_511[j]) {
                                if (_s_110.find(next)->second!=_s_111->_s_98()._s_34()) {
                                    _s_550 = true;
                                }
                            }
                        }
                    }
                    if (!_s_550) {
                        _s_426 = false;
                        _s_511[i] = false;
                    }
                }
            }
        }
    } while (!_s_426);
    do {
        _s_426 = true;
        for (uint i=0; i<_s_108.size(); i++) {
            if ((!(_s_511[i])) && (_s_129.count(i)==0)) {
                bool _s_552 = false;
                std::pair<uint,uint> _s_551(i,i);
                std::map<std::pair<uint,uint>,_s_27>::const_iterator it = _s_110.find(_s_551);
                if ((it!=_s_110.end()) && (it->second!=_s_111->_s_98()._s_34())) {
                    for (uint j=0; j<_s_108.size(); j++) {
                        if (i!=j) {
                            if ((!(_s_511[j])) && (_s_129.count(j)==0)) {
                                std::pair<uint,uint> _s_553(i,j);
                                std::map<std::pair<uint,uint>,_s_27>::const_iterator it2 = _s_110.find(_s_553);
                                if ((it2!=_s_110.end()) || (it2->second!=_s_111->_s_98()._s_34())) {
                                    _s_552 = true;
                                }
                            }
                        }
                    }
                }
                if (!_s_552) {
                    _s_426 = false;
                    _s_129.insert(i);
                }
            }
        }
    } while (!_s_426);
}
void _s_101::_s_102(_s_27 _s_142, std::vector<std::string> &_s_103, uint _s_104, std::string _s_105, std::string _s_106, std::ostringstream &_s_107) const {
    if (_s_104>=_s_103.size()) {
        if (_s_142==_s_111->_s_98()._s_33()) {
            _s_107 << _s_105;
            if ((_s_105[_s_105.length()-1]) == '(') _s_107 << "1";
            _s_107 << _s_106;
            return;
        } else if (_s_142==_s_111->_s_98()._s_34()) {
            return;
        } else {
            throw _s_2("toPromelaNeverClaimRecurse: Variable list is incomplete!");
        }
    }
    _s_27 _s_554 = _s_111->_s_99(_s_103[_s_104]);
    _s_27 _s_584 = (_s_142 & _s_554)._s_72(_s_554);
    _s_27 _s_585 = (_s_142 & (!_s_554))._s_72(_s_554);
    if (_s_584==_s_585) {
        _s_102(_s_142, _s_103, _s_104+1, _s_105, _s_106, _s_107);
    } else {
        std::string _s_555 = _s_105;
        if ((_s_555[_s_555.length()-1]) != '(') _s_555 = _s_555 + " && ";
        _s_555 = _s_555 + _s_103[_s_104];
        _s_102(_s_584, _s_103, _s_104+1, _s_555, _s_106, _s_107);
        std::string _s_556 = _s_105;
        if((_s_556[_s_556.length()-1]) != '(') _s_556 = _s_556 + " && ";
        _s_556 = _s_556 + "!" + _s_103[_s_104];
        _s_102(_s_585, _s_103, _s_104+1, _s_556, _s_106, _s_107);
    }
}
void _s_101::_s_1(std::vector<bool> &_s_59) const {
    _s_59.clear();
    uint size = _s_108.size();
    bool *_s_557 = new bool[size*size];
    for (uint x=0; x<size; x++) {
        for (uint y=0; y<size; y++) {
            if (x==y) {
                _s_557[x*size+y] = true;
            } else {
                _s_557[x*size+y] = _s_139(x,y)!=_s_111->_s_98()._s_34();
            }
        }
    }
    bool _s_558;
    do {
        _s_558 = false;
        for (uint x=0; x<size; x++) {
            for (uint y=0; y<size; y++) {
                if (x!=y) {
                    for (uint z=0; z<size; z++) {
                        if ((x!=z) && (y!=z)) {
                            if (_s_557[x*size+y] && _s_557[y*size+z] && (!(_s_557[x*size+z]))) {
                                _s_557[x*size+z] = true;
                                _s_558 = true;
                            }
                        }
                    }
                }
            }
        }
    } while (_s_558);
    std::set<uint> _s_559;
    std::map<uint,bool> _s_560;
    std::map<uint,uint> _s_561;
    std::map<uint,uint> _s_562;
    for (uint i=0; i<size; i++) {
        int _s_563 = -1;
        for (uint j=0; (j<i) && (_s_563==-1); j++) {
            if (_s_557[i*size+j] && _s_557[j*size+i]) {
                _s_563 = j;
            }
        }
        if (_s_563==-1) {
            _s_559.insert(i);
            _s_561[i] = i;
            _s_560[i] = _s_109[i];
            _s_562[i] = 1;
        } else {
            _s_561[i] = _s_563;
            _s_560[_s_563] |= _s_109[i];
            _s_562[_s_563]++;
        }
    }
    for (uint i=0; i<size; i++) {
        if (_s_562[_s_561[i]]==1) {
            _s_59.push_back((!_s_560[i]) || (_s_139(i,i)==_s_111->_s_98()._s_34()));
        } else {
            _s_59.push_back((!_s_560[_s_561[i]]));
        }
        if (_s_59.back()) {
            std::cout << "Marked state as counter-less: " << _s_108[i] << "\n";
        } else {
            std::cout << "State needs counter: " << _s_108[i] << "\n";
        }
    }
    delete[] _s_557;
}
