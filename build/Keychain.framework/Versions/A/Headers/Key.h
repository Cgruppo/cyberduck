//
//  Key.h
//  Keychain
//
//  Created by Wade Tregaskis on Fri Jan 24 2003.
//
//  Copyright (c) 2003, Wade Tregaskis.  All rights reserved.
//  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
//    * Neither the name of Wade Tregaskis nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#import <Keychain/NSCachedObject.h>
#import <Keychain/CSSMModule.h>

#import <Foundation/Foundation.h>
#import <Security/Security.h>


/*! @class Key
    @abstract The cornerstone of most cryptographic operations.
    @discussion A detailed discussion of a key is not really required, given it is such an obvious concept.  Check out any introduction to cryptography for more information on types of keys, their functions, and so forth.  This is more of a requirement than a suggestion - don't go creating and using keys without knowledge of their security, particularly in regards to what algorithms and key sizes are considered 'safe'. */

@interface Key : NSCachedObject {
    CSSMModule *_CSPModule;
    SecKeyRef _key;
    const CSSM_KEY *_CSSMKey;
    int _error;
}

/*! @method keyWithKeyRef:
    @abstract Creates and returns a Key instance based on a SecKeyRef.
    @discussion The SecKeyRef is retained by the new Key instance for the duration of it's life.  This method caches existing Key instances, such that multiple calls with the same SecKeyRef will return the same unique Key instance.
    @param ke The SecKeyRef.
    @param CSPModule The CSP module the given key is a member of.  Should not be nil.
    @result If a Key instance already returns for the given SecKeyRef, returns that existing instance.  Otherwise, creates a new instance and returns it.  In case of error, returns nil. */

+ (Key*)keyWithKeyRef:(SecKeyRef)ke module:(CSSMModule*)CSPModule;

/*! @method keyWithCSSMKey:
    @abstract Creates and returns a Key instance based on a CSSM_KEY.
    @discussion The CSSM_KEY provided must not be destroyed while the returned object still exists.  Conversely, the returned object will never destroy the CSSM_KEY itself.
    @param ke The CSSM_KEY.
    @param CSPModule The CSP module the given key is a member of.  Should not be nil.
    @result If successful, a new Key instance based on the provided CSSM_KEY.  Otherwise, returns nil. */

+ (Key*)keyWithCSSMKey:(const CSSM_KEY *)ke module:(CSSMModule*)CSPModule;

/*! @method initWithKeyRef:
    @abstract Initiailizes the receiver with a SecKeyRef.
    @discussion The SecKeyRef is retained by the receiver for the duration of it's lifetime.  Changes to the SecKeyRef will reflect on the receiver, and vice versa.  Note that this method caches existing Key instances, such that calling this with a SecKeyRef that has already been used will release the receiver and return the existing instance.
    @param ke The SecKeyRef.
    @param CSPModule The CSP module the given key is a member of.  Should not be nil.
    @result If SecKeyRef is a valid key, returns the receiver.  Otherwise, releases the receiver and returns nil. */

- (Key*)initWithKeyRef:(SecKeyRef)ke module:(CSSMModule*)CSPModule;

/*! @method initWithCSSMKey:
    @abstract Initializes the receiver with a CSSM_KEY
    @discussion The CSSM_KEY provided must not be destroyed while the receiver still exists.  Conversely, the receiver will not automatically destroy the key itself.
    @param ke The CSSM_KEY.
    @param CSPModule The CSP module the given key is a member of.  Should not be nil.
    @result If successful, returns the receiver.  Otherwise, releases the receiver and returns nil. */

- (Key*)initWithCSSMKey:(const CSSM_KEY *)ke module:(CSSMModule*)CSPModule;

/*! @method version
    @abstract Returns the version of the CDSA used to generate the key.
    @discussion Be wary with this - this framework tries to preserve the version value for keys, but many other libraries do not (e.g. Apple's own Security framework).  In fact, many real world applications provide no indication of such information to start with.  Plus, this version number only applies to keys generated by the CDSA, not from other libraries.  So, in summary, be wary of using this for anything important, as in a significant number of cases it may not be valid.

                Generally the fallback is to the version of the CDSA under which this framework was compiled (2.0 at the time of writing).  This fallback behaviour, at least, is fairly consistant across libraries (e.g. Apple's Security framework, again).
    @result The returned value is really just a uint32 describing the major version only. */

/*! @method init
    @abstract Reject initialiser.
    @discussion You cannot initialise a Key using "init" - use one of the other initialisation methods.
    @result This method always releases the receiver and returns nil. */

- (Key*)init;

/*! @method CSPModule
    @abstract Returns the CSP module in which the key resides.
    @discussion It is conceivable that keys can exist outside any particular CSP module, in various raw and wrapped forms.  Thus, this method may well return nil.  However, for keys which are just references inside a particular module, this method returns the module in which they exist.
    @param Returns the CSP module (if any) the key resides within, or nil if there is no CSP module associated with the key or an error occurs. */

- (CSSMModule*)CSPModule;

- (CSSM_HEADERVERSION)version;

/*! @method blobType
    @abstract Returns the 'blob' type of the receiver.
    @discussion The 'blob' type of a key is how it is stored in memory - whether as a reference, in raw form, or wrapped form.  See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for valid types. */

- (CSSM_KEYBLOB_TYPE)blobType;

/*! @method format
    @abstract Returns the format of the receiver.
    @discussion See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for known formats. */

- (CSSM_KEYBLOB_FORMAT)format;

/*! @method algorithm
    @abstract Returns the algorithm of the receiver.
    @discussion See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for algorithms.  If you want to know the algorithm used to wrap a key, see the wrapAlgorithm method. */

- (CSSM_ALGORITHMS)algorithm;

/*! @method wrapAlgorithm
    @abstract Returns the algorithm used to wrap the receiver.
    @discussion See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for algorithms. */

- (CSSM_ALGORITHMS)wrapAlgorithm;

/*! @method keyClass
    @abstract Returns the receiver's key class.
    @discussion The key class is simply the general type of key - e.g. symmetric, public, private, etc.  See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for known classes. */

- (CSSM_KEYCLASS)keyClass;

/*! @method logicalSize
    @abstract Returns the logical size of the receiver in bits.
    @discussion Different algorithms use keys in different ways, such that a key which has a certain number of physical bits may 'expand out to', or 'derive', or 'represent', more bits.  The logical size is what is usually quoted in most applications, e.g. "1024-bit RSA" or "128-bit AES" etc.  For some algorithms, the physical key size may not even be constant, for the same logical key size.
    @result The logical size of the receiver in bits. */

- (int)logicalSize;

/*! @method attributes
    @abstract Returns a mask representing the attributes of the receiver.
    @discussion See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for masks.
    @result The mask, which at time of writing is just a uint32. */

- (CSSM_KEYATTR_FLAGS)attributes;

/*! @method usage
    @abstract Returns a mask representing the valid uses for the receiver.
    @discussion See <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a> for masks.
    @result The mask, which at time of writing is just a uint32. */

- (CSSM_KEYUSE)usage;

/*! @method startDate
    @abstract Returns the time and date before which the receiver should be considered invalid.
    @discussion You should not use a key prior to it's start date, because there may well be a good reason it is not valid until then.  This framework does not, at present, verify key validity dates, so you must do so yourself.  You should verify the start date at least once, when you first use it.  If the start date is in the past, you shouldn't, in normal use, need to verify it again.  If not, you should fail in whatever operation you were planning to perform, and remember to test the start date again next time you try to use the key.
    @result Returns the start date in local time. */

- (NSCalendarDate*)startDate;

/*! @method endDate
    @abstract Returns the time and date after which the receiver should be considered invalid.
    @discussion You should not use a key after it's end date, because there may well be a good reason it expired at that time.  This framework does not, at present, verify key validity dates, so you must do so yourself.  It may not be necessary to verify a key each time it is used, depending on your application.  It is however a good idea to verify the key every now and again, if your application will run for more than half an hour at a time.
    @result Returns the end date in local time. */

- (NSCalendarDate*)endDate;

/*! @method wrapMode
    @abstract Returns the wrapping mode, if any, of the receiver.
    @discussion You can view a list of known wrapping modes in <a href="file:///System/Library/Frameworks/Security.framework/Headers/cssmtype.h>cssmtype.h</a>.
    @result Returns the wrapping mode, or CSSM_ALGMODE_NONE if the receiver is not a wrapped key. */

- (CSSM_ENCRYPT_MODE)wrapMode;

/*! @method description
    @abstract Returns a user-readable description of the receiver.
    @discussion This makes a reasonable attempt to present the key's properties in a human-readable form.  Note that it does not contain the key's data.  If you want the raw key data, see the rawData method.
    @result An NSString containing a human-readable description of the receiver. */

- (NSString*)description;

/*! @method wrappedKeyUnsafe
    @abstract Returns a clear (raw) version of the receiver.
    @discussion You really shouldn't use this function on symmetric or private keys, only public ones.  Null wrapping symmetric or private keys is a bad idea, unless you absolutely have to for some amazingly estoric scenario.  Try to use wrappedKeyUsingKey: instead.

                Note that you cannot wrap a wrapped key.
    @result Returns a new Key instance containing the clear (null wrapped) version of the receiver. */

- (Key*)wrappedKeyUnsafe;

/*! @method wrappedKeyUnsafeWithDescription:
    @abstract Returns a clear (raw) version of the receiver.
    @discussion You really shouldn't use this function on symmetric or private keys, only public ones.  Null wrapping symmetric or private keys is a bad idea, unless you absolutely have to for some amazingly estoric scenario.  Try to use wrappedKeyUsingKey: instead.

                Note that you cannot wrap a wrapped key.
    @param description A textual description to be attached to the wrapped key.  This may or may not come out in the unwrapping.  Best not to use it until it's behaviour is properly determined.
    @result Returns a new Key instance containing the clear (null wrapped) version of the receiver. */

- (Key*)wrappedKeyUnsafeWithDescription:(NSString*)description;

/*! @method wrappedKeyUsingKey:description:
    @abstract Returns the receiver wrapped using a symmetric or public key.
    @discussion This method returns a Key that is the receiver wrapped with the key provided.  Wrapped keys can be serialized for storage and transmission.  If you are storing a key, you should wrap it with a symmetric key you derive from a user provided passphrase, or some other such mechanism.  If you are transmitting a key to another location, you can adopt the same approach, although a better one is to wrap the key using a public key.

                Note that you cannot wrap a wrapped key.
    @param wrappingKey The key to be used to wrap the receiver.  This may be a symmetric or a public key.
    @result Returns a new Key instance if successful, nil otherwise. */

- (Key*)wrappedKeyUsingKey:(Key*)wrappingKey;

/*! @method wrappedKeyUsingKey:description:
    @abstract Returns the receiver wrapped using a symmetric or public key, with the provided description attached.
    @discussion This method returns a Key that is the receiver wrapped with the key provided.  Wrapped keys can be serialized for storage and transmission.  If you are storing a key, you should wrap it with a symmetric key you derive from a user provided passphrase, or some other such mechanism.  If you are transmitting a key to another location, you can adopt the same approach, although a better one is to wrap the key using a public key.

                Note that you cannot wrap a wrapped key.
    @param wrappingKey The key to be used to wrap the receiver.  This may be a symmetric or a public key.
    @param description A textual description to be associated with the wrapped key.  This may or may not have any use, and may not come out in the unwrapping.  Until this is tested, it's generally best to stick to the wrappedKeyUsingKey: method.
    @result Returns a new Key instance if successful, nil otherwise. */

- (Key*)wrappedKeyUsingKey:(Key*)wrappingKey description:(NSString*)description;

/*! @method unwrappedKeyUnsafe
    @abstract Returns the unwrapped version of the receiver.
    @discussion This method unwraps 'raw' or 'null-wrapped' or 'clear' keys.  It is labeled unsafe because in many cases, such an operation <i>is</i> unsafe.  Public keys are the exception to this of course, because they are obviously designed to be shared freely.  But symmetric and private keys should very rarely be exported from an application without being protected in some way, such as with another key.  They certainly should never be stored or transmitted in such a form.  The following rant deals only with symmetric and private keys:

                There is almost no excuse for why you cannot use another key to wrap a key.  If nothing else, you can derive a suitable key from a user-supplied passphrase, which at least provides some level of protection.  Further excuses like "the user might forget the passphrase" are not acceptable.  How about, "some criminal might get your key and steal your identity"?  Or a little more light-sidedly "your friends may get your keys and send doctored photo's of you to that girl you've got a crush on.  Photo's of you doing unspeakable things to innocent barn yard animals.  Photo's signed by your private key, and thus 'unforgeable'.".  Whether criminal or comical, the outcome of poor key security is not fun for those who own the key.

                The fundamental point is that no matter how good a security framework is technically, it means exactly jack all if the implementors do stupid things, like permit symmetric or private keys to be transmitted or stored in the clear.  Don't do it.  If you do, elves will sneak into your computer at night and put playdoh in your power supply.
    @result If the receiver is a null-wrapped key, returns a new Key instance for the unwrapped key.  Otherwise, returns nil. */

- (Key*)unwrappedKeyUnsafe;

/*! @method unwrappedKeyUsingKey:
    @abstract Returns the unwrapped version of the receiver.
    @discussion This method only works for receivers containing wrapped key data.  It's returns a new Key instance containing the unwrapped key.
    @param wrappingKey The key to be used to unwrap the receiver.  This may be a symmetric or public key, as appropriate for the type of wrapping used on the receiver.  This cannot be NULL.
    @result If the receiver is a wrapped key and the provided key is capable of unwrapping it, returns a new Key instance for the unwrapped key.  Otherwise, returns nil. */

- (Key*)unwrappedKeyUsingKey:(Key*)wrappingKey;

/*! @method keyHash
    @abstract Returns a hash of the key, suitable for uniquely identifying the key (not the object, unlike the NSObject hash method).
    @discussion This method returns a hash of the receiver's contents.  It does not work the same as the generic NSObject hash function, and should not be treated as such.  You may wish to use a public key's hash, for example, when referring to it from the corresponding private key.
    @result Returns the hash of the receiver. */

- (NSData*)keyHash;

/*! @method rawData
    @abstract Returns the key data itself, free of any of it's attributes.
    @discussion No encoding is performed - only the actual key data itself is returned, without any extra encoding [as with the data method].
    @result If the receiver is a key in raw form, an NSData instance is returned containing just the raw key data itself.  Otherwise, nil is returned. */

- (NSData*)rawData;

/*! @method data
    @abstract Returns the key in a reliable raw data form.
    @discussion This method encodes both the key data and it's associated attributes into a simple data form.  There is no standard form for serially-representing a key like this.  Many real-world applications maintain a separation between the key data and it's attributes, preferring to 'remember' the attributes separately from the data, or re-generate them as required.  This appears to be counter-intuitive.  Thus, this method is suggested as a standard means for serially encoding keys.

                Note that at present the data generated by this function is not byte-order independent, meaning it cannot necessarily be taken from one platform to another.  It is also, at present, not necessarily compiler independent.  These issues will be resolved in the near future.  In the mean time, it is still safe to use this method if you are doing single-system testing.  Future versions will be able to read any data stored using this version, provided you do so on the same platform and with the same compiler as was used to generate the data.
    @result If the receiver is a key in raw form, an NSData instance is returned containing all the encoded data.  Otherwise, nil is returned. */

- (NSData*)data;

/*! @method isEqualToKey:
    @abstract Compares two keys and returns whether or not they are exactly identical.
    @discussion For this method to consider two keys to be identical they must have exactly the same data and exactly the same header information - i.e. usage, attributes, version, validity, etc.  If you wish to compare two keys based on whether they have the same key data, use isSimilarToKey:.

                Note that this method only works for wrapped/raw keys.
    @param key The key to compare with the receiver.
    @result YES if the two keys are exactly the same, NO otherwise. */

- (BOOL)isEqualToKey:(Key*)otherKey;

/*! @method isSimilarToKey:
    @abstract Compares two keys and returns whether or not they are similar (have the same key data).
    @discussion Unlike the isEqualToKey: method, this method only compares the keys' raw data.  Thus, their header info may differ (i.e. usage, attributes, version, validity, etc).  This is useful in many cases because if the raw data is the same, the key is essentially the same - it will produce exactly the same cryptographic outputs.  It is entirely plausible that you will have two distinct copies of the same key, but each with different attributes.

                Note that this method only works for wrapped/raw keys.
    @param key The key to compare to the receiver.
    @result YES if the keys' raw data is identical, NO otherwise. */

- (BOOL)isSimilarToKey:(Key*)otherKey;

/*! @method lastError
    @abstract Returns the last error that occured for the receiver.
    @discussion The set of error codes encompasses those returned by Sec* functions - refer to the Security framework documentation for a list.  At present there are no other error codes defined for Access instances.

                Please note that this error code is local to the receiver only, and not any sort of shared global value.
    @result The last error that occured, or zero if the last operation was successful. */

- (int)lastError;

/*! @method CSSMKey
    @abstract Returns the CSSM_KEY for the receiver.
    @discussion This function works whether you created the object with a CSSM_KEY or SecKeyRef.  Please make sure to copy the returned CSSM_KEY if you want to change it - don't just cast away the const.  Changing the returned CSSM_KEY directly may result in unfavourable behaviour of the owning Key instance.
    @result The CSSM_KEY for the receiver. */

- (const CSSM_KEY *)CSSMKey;

/*! @method keyRef
    @abstract Returns a SecKeyRef representing this key, if one exists.
    @discussion Keys can be created from SecKeyRef's or from CSSM_KEY's.  At present, while you can get the CSSM_KEY from a SecKeyRef-created instance, you cannot get a SecKeyRef from a CSSM_KEY-created instance.  This is considered a bug, but may not be fixed in the immediate future.

                Note that the returned object is linked to the receiver, and changes to the object will reflect in the receiver (and vice versa).
    @result The SecKeyRef representing the receiver, or nil if the receiver was not created with one. */
- (SecKeyRef)keyRef;

@end
